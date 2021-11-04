package bean2case

import java.beans.{BeanInfo, Introspector, PropertyDescriptor}
import java.lang.reflect.{Parameter, ParameterizedType, Type}


class BeanContext(context: CodeContext, beanInfo: BeanInfo) {

  val clazz: Class[_] = beanInfo.getBeanDescriptor.getBeanClass
  def scalaPackage: String = clazz.getPackage.getName + "_scala"
  def scalaSimpleName: String = clazz.getSimpleName
  def javaQualifiedName: String = clazz.getName
  def scalaQualifiedName: String = scalaPackage + "." + scalaSimpleName
  def fields: List[FieldContext] = beanInfo.getPropertyDescriptors.toList
    .filter(p => p.getReadMethod != null && p.getWriteMethod != null)
    .map(new FieldContext(context, _))

}

/**
 * Java Primitives
 * String, Date, Timestamp, BigDecimal, BigNumber,
 * List/Array
 * Map
 * otherwise treat like JavaBean
 */
case class FieldContext(context: CodeContext, property: PropertyDescriptor) {
  val generic = property.getReadMethod.getGenericReturnType
  val mapping = context.getMapping(generic)
  def scalaType: String = mapping.scalaTypeName
  def javaSetter: String = property.getWriteMethod.getName
  def javaGetter: String = property.getReadMethod.getName
  def name = property.getName

  def j2s(expr: String): String =  mapping.j2s(expr)

  def s2j(expr: String): String = mapping.s2j(expr)
}

case class ServiceContext(context:CodeContext, clazz: Class[_]) {
  assert(clazz.isInterface)
  def scalaPackage: String = clazz.getPackage.getName + "_scala"
  def scalaSimpleName: String = clazz.getSimpleName
  def scalaQualifiedName: String = scalaPackage + "." + scalaSimpleName
  def javaQualifiedName: String = clazz.getName

  def methods: List[MethodContext] = clazz.getMethods.filterNot(_.isDefault)
    .toList.map(MethodContext(context, _))
}

case class ParameterContext(context: CodeContext, parameter: Parameter, owner: MethodContext) {
  val mapping = context.getMapping(parameter.getType)
  def name: String = parameter.getName
  def javaQualifiedName: String = CodeContext.asJavaTypeName(parameter.getParameterizedType)
  def scalaTypeName: String = mapping.scalaTypeName
  def isLast: Boolean = {
    val paras = owner.method.getParameters
    parameter == paras(paras.length-1)
  }

  def j2s(v: String): String = mapping.j2s(v)
  def s2j(v: String): String = mapping.s2j(v)
}

case class MethodContext(context: CodeContext, method: java.lang.reflect.Method) {

  def name: String = method.getName
  def parameters: List[ParameterContext] = method.getParameters.toList.map(ParameterContext(context,_, this))

  def returnScalaType: String = {
    val mapping = context.getMapping(method.getGenericReturnType)
    mapping.scalaTypeName
  }
  def returnJavaQualifiedName: String = CodeContext.asJavaTypeName(method.getGenericReturnType)

}

case class TypeMapping (
  val javaType: Type,
  val scalaTypeName: String,
  j2s: String=> String,
  s2j: String=> String,
  generate: Boolean = false
  )

object CodeContext {
  val identity: String=>String = (x)=>x

  def asJavaTypeName(typ: Type): String = typ match {
    case java.lang.Byte.TYPE => "scala.Byte"
    case java.lang.Boolean.TYPE => "scala.Boolean"
    case java.lang.Short.TYPE => "scala.Short"
    case java.lang.Character.TYPE => "scala.Short"
    case java.lang.Integer.TYPE => "scala.Int"
    case java.lang.Float.TYPE => "scala.Float"
    case java.lang.Double.TYPE => "scala.Double"
    case java.lang.Long.TYPE => "scala.Long"
    case java.lang.Void.TYPE => "scala.Unit"
    case _ => typ.getTypeName
  }
}

class CodeContext {

  import CodeContext.identity

  type JavaClass = Class[_]
  type ScalaClass = Class[_]

  /**
   * classes under these packages can be treat as beans, exclude Enums.
   *
   * Others will be treat as sealed values, which don't generate scala Mapping
   */
  val _beanMappingPackages = scala.collection.mutable.ListBuffer[String]()

  /**
   * forced as beans
   */
  val _includes = scala.collection.mutable.ListBuffer[Class[_]]()
  val _excludes = scala.collection.mutable.ListBuffer[Class[_]]()

  // Map: primitive
  // Map: Collections
  // Map: Java Bean

  val mappings: collection.mutable.ListBuffer[TypeMapping] = collection.mutable.ListBuffer[TypeMapping]() :+
    TypeMapping(java.lang.Boolean.TYPE, "scala.Boolean", identity, identity) :+
    TypeMapping(java.lang.Byte.TYPE, "scala.Byte", identity, identity) :+
    TypeMapping(java.lang.Short.TYPE, "scala.Short", identity, identity) :+
    TypeMapping(java.lang.Character.TYPE, "scala.Char", identity, identity) :+
    TypeMapping(java.lang.Integer.TYPE, "scala.Int", identity, identity) :+
    TypeMapping(java.lang.Float.TYPE, "scala.Float", identity, identity) :+
    TypeMapping(java.lang.Double.TYPE, "scala.Double", identity, identity) :+
    TypeMapping(java.lang.Long.TYPE, "scala.Long", identity, identity) :+
    TypeMapping(classOf[java.lang.String], "java.lang.String", identity, identity) :+
    TypeMapping(classOf[java.math.BigInteger], "scala.BigInt", (v)=>s"BigInt($v)", (v)=>s"$v.bigInteger") :+
    TypeMapping(classOf[java.math.BigDecimal], "scala.BigDecimal", (v)=>s"BigDecimal($v)", (v)=>s"$v.bigDecimal")

  val mappingsByJavaType = collection.mutable.Map[Type, TypeMapping]() ++ mappings.map(x => (x.javaType, x))

  def beanMappingPackages(packages: String*): CodeContext = {
    this._beanMappingPackages ++= packages
    this
  }
  def includes(types: Class[_]*): CodeContext = {
    this._includes ++= types
    this
  }
  def exclude(types: Class[_]*): CodeContext = {
    this._excludes ++= types
    this
  }


  def getMapping(javaType: Type): TypeMapping = {
    mappingsByJavaType.get(javaType) match {
      case Some(mapping) => mapping
      case None =>
        val mapping = createMapping(javaType)
        mappings :+ mapping
        mappingsByJavaType += (javaType -> mapping)
        mapping
    }
  }

  private def createMapping(javaType: Type): TypeMapping = {
    javaType match {
      case javaType: Class[_] =>
        if (_excludes.contains(javaType)) TypeMapping(javaType, javaType.getName, identity, identity)
        else if (_includes.contains(javaType) || _beanMappingPackages.exists(pkg => isPkgMatched(javaType, pkg))) {
          val packageName = javaType.getPackage.getName
          val className = javaType.getSimpleName
          val scalaPackage = packageName + "_scala"
          val scalaTypeName = s"${scalaPackage}.${className}"
          val j2s = (v: String) => s"($v:${scalaTypeName})"
          val s2j = (v: String) => s"($v:${javaType.getName})"
          TypeMapping(javaType, scalaTypeName, j2s, s2j, true)
        }
        else TypeMapping(javaType, javaType.getName, identity, identity)
      case pt: ParameterizedType =>
        val rawType = pt.getRawType
        val argTypes = pt.getActualTypeArguments
        assert(rawType == classOf[java.util.List[_]])
        assert( argTypes(0).isInstanceOf[Class[_]])

        val childMapping = getMapping(argTypes(0))
        val j2s = (v: String) => s"$v.asScala.toList.map(implicitly[${childMapping.javaType.getTypeName}=>${childMapping.scalaTypeName}])"
        val s2j = (v: String) => s"$v.map(implicitly[${childMapping.scalaTypeName}=>${childMapping.javaType.getTypeName}]).asJava"
        TypeMapping(pt, s"scala.List[${childMapping.scalaTypeName}]", j2s, s2j, false)
    }
  }


  private def isPkgMatched(clazz: JavaClass, pkg: String): Boolean = {
    val clazzPkg = clazz.getPackage.getName
    pkg == clazzPkg || (clazzPkg.contains(s"${pkg}."))
  }

//  def getScalaTypeNameFor(javaType: Class[_]): String = {
//    mappingsByJavaType.get(javaType) match {
//      case Some(mapping) => mapping.scalaTypeName
//      case None => ???
//    }
//  }


}