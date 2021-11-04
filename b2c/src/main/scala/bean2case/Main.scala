package bean2case

import java.beans.{BeanInfo, Introspector}
import java.io.{File, FileOutputStream, PrintWriter}
import org.scalafmt.interfaces.Scalafmt

import java.nio.file.Paths

object SourceGenerator {

  val scalafmt = Scalafmt.create(this.getClass.getClassLoader)
  val config = Paths.get(".scalafmt.conf")

  def saveFile(dir: File, pkg: String, fileName: String, code: String): Unit ={

    val source = Paths.get("main.scala")
    val fmtcode = scalafmt.format(config, source, code)

    new File(dir, pkg.replace(".", "/")).mkdirs()

    val file = new File(dir, pkg.replace(".", "/") +
      "/" + fileName)
    val writer = new PrintWriter(new FileOutputStream(file))

    writer.write(fmtcode)
    writer.close()
  }

}

class Bean2Case(context: CodeContext, beanClass: Class[_]) {

  context.includes(beanClass)

  def generate(dir: File): Unit = {
    val beanContext = new BeanContext(context, Introspector.getBeanInfo(beanClass))
    val template = txt.BeanExtension.apply(beanContext)

    SourceGenerator.saveFile(dir, beanContext.scalaPackage, beanContext.scalaSimpleName + ".scala",
      template.toString())

  }
}

class ServiceCompanion(context: CodeContext, serviceClass: Class[_]) {

  def generate(dir: File): Unit = {

    val serviceContext = new ServiceContext(context, serviceClass)
    val template = txt.ServiceExtension.apply(serviceContext)

    SourceGenerator.saveFile(dir, serviceContext.scalaPackage,
      serviceContext.scalaSimpleName + ".scala", template.toString())

  }

}
