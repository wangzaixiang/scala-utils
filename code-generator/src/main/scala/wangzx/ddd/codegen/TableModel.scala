package wangzx.ddd.codegen

import java.sql.Types

object TableModel {

  def underscoreToCamel(str: String) : String = {
    val builder = StringBuilder.newBuilder
    var lastChar: Char = 0
    for(ch <- str) {
      ch match {
        case '_' =>
        case _ =>
          if(lastChar == '_') builder.append(Character.toUpperCase(ch))
          else builder.append(ch)
      }
      lastChar = ch
    }
    builder.toString()
  }

}

/**
 * Created by wangzx on 15/8/7.
 */
class TableModel {
  var remarks: String = _
  var tablename: String = _
  var catelog: String =_
  var schema: String = _
  var columns: List[ColumnModel] = Nil

  def entityName = TableModel underscoreToCamel  tablename.capitalize
}

class ColumnModel {
  var columnName: String = _
  var datatype: Int = _
  var typename: String = _
  var size: Int = _
  var decimal_digits: Int = _
  var nullable: Boolean = _
  var remarks: String =_
  var ordinal_position: Int = _
  var autoincrement: Boolean = _
  var isId: Boolean = _

  def fieldName = columnName match {
    case x if Set("type").contains(x) => "`%s`" format x
    case x if x.contains("_") => TableModel underscoreToCamel x
    case _ => columnName
  }
  def fieldType = datatype match {
    case Types.INTEGER | Types.TINYINT | Types.BIT | Types.SMALLINT => "Int"
    case Types.BIGINT => "Long"
    case Types.CHAR => "String"
    case Types.VARCHAR | Types.LONGVARCHAR => "String"
    case Types.DATE => "java.sql.Date"
    case Types.TIMESTAMP => "java.sql.Timestamp"
    case Types.DOUBLE => "Double"
    case Types.DECIMAL => "BigDecimal"
    case Types.LONGVARBINARY => "Array[Byte]"
    case _ => throw new AssertionError("check type " + datatype + ":" + typename)
  }



}

