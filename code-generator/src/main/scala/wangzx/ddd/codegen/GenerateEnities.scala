package wangzx.ddd.codegen

import java.io.FileOutputStream
import java.sql.{DatabaseMetaData, DriverManager, ResultSet}

import scala.collection.mutable.ListBuffer

/**
 * Created by wangzx on 15/8/7.
 */
object GenerateEnities {

    implicit def enhance(rs:ResultSet) = new {
      def foreach(f: ResultSet=>Unit) {
        while(rs.next){
          f(rs)
        }
      }
      def map[T] (f: ResultSet=>T): List[T] = {
        val b = new ListBuffer[T]
        while(rs.next){
          b += f(rs)
        }
        b.toList
      }
    }

    def main(args: Array[String]) {
      val url = "jdbc:mysql://localhost/orderdb?useUnicode=true&characterEncoding=utf8&autoReconnect=true"

      val driver = new com.mysql.jdbc.Driver
      val username = "root"
      val password = "root"

      val conn = DriverManager.getConnection(url, username, password)

      val dbmeta =conn.getMetaData
      val tables = dbmeta.getTables(null, null, "%", Array("TABLE"))
      tables.foreach { rs=>
        val tableModel = new TableModel
        import tableModel._
        catelog = rs.getString("TABLE_CAT")
        schema = rs.getString("TABLE_SCHEM")
        tablename = rs.getString("TABLE_NAME")
        remarks = rs.getString("REMARKS")

        println()
        println("TABLE " + tablename)

        val primaryKey = dbmeta.getPrimaryKeys(catelog, schema, tablename)
        val pk_fields =primaryKey.map { rs=>
          (rs.getString("COLUMN_NAME"), rs.getInt("KEY_SEQ"))
        }.toMap


        val columns = dbmeta.getColumns(catelog, schema, tablename, "%")
        columns.foreach { crs=>

          val columnModel = new ColumnModel
          import columnModel._

          columnName = crs.getString("COLUMN_NAME")
          datatype = crs.getInt("DATA_TYPE")
          typename = crs.getString("TYPE_NAME")
          size = crs.getInt("COLUMN_SIZE")
          decimal_digits = crs.getInt("DECIMAL_DIGITS")
          nullable = crs.getInt("NULLABLE") == DatabaseMetaData.columnNullable
          remarks = crs.getString("REMARKS")
          ordinal_position = crs.getInt("ORDINAL_POSITION")
          autoincrement = crs.getString("IS_AUTOINCREMENT") == "YES"
          isId = pk_fields contains columnName

          tableModel.columns = columnModel :: tableModel.columns

          // println("%s\t%d(%s)\t%d(%d)\t%s\t%s" format (name, datatype, typename, size, decimal_digits, nullable, remarks))

        }

        tableModel.columns = tableModel.columns.sortBy(_.ordinal_position)

        val source = txt.Entity.apply(tableModel)

        val out = new FileOutputStream("code-generator/src/test/scala/gen/" + tableModel.entityName + ".scala")
        out.write(source.toString.getBytes("UTF-8"))
        out.close

      }

    }

}
