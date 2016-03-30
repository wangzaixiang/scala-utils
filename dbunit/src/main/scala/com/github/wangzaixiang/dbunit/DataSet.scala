package com.github.wangzaixiang.dbunit

import java.io.Serializable
import java.sql._
import java.util
import javax.sql.DataSource

import wangzx.scala_commons.sql.Row.{BooleanCell, TimestampCell}
import wangzx.scala_commons.sql._

import scala.xml._

/**
 * Created by wangzx on 15/7/21.
 */
object DataSet {

  case class Record(table: String, rowData: Row)

  def apply(table: String, rows: List[Row]): DataSet =
    new DataSet( rows.map(row => Record(table, row)) )

  def dumpXml(tagName: String, rows: List[Row]): NodeSeq = rows.map { row =>
    val attributes = row.cells.foldRight(Null: MetaData) { (cell: Row.Cell[_], prev: MetaData) =>
      Attribute(null, cell.name, cell.getString, prev)
    }
      <some/>.copy(label = tagName).copy(attributes = attributes)
  }

  // a first empty row <user/> will always perform a REFRESH operation
  def save(dataSource: DataSource, dataSet: DataSet, syncMode: DataSetSyncMode = INSERT_UPDATE): Unit = {

    val cleanHistory = collection.mutable.Set[String]()
    dataSource.withConnection { conn =>

      dataSet.records.foreach { record =>

        if (record.rowData == null) {
          cleanData(conn, record.table)
        }
        else {
          syncMode match {
            case INSERT => insertData(conn, record)
            case UPDATE => updateData(conn, record)
            case INSERT_UPDATE => insertUpdate(conn, record)
            case REFRESH =>
              if (!(cleanHistory contains record.table)) {
                cleanData(conn, record.table)
                cleanHistory.add(record.table)
              }
              insertData(conn, record)
          }
        }
      }
    }

  }

  private def cleanData(conn: Connection, table: String) =
    conn.executeUpdate(s"delete from $table where 1 = 1")


  private def insertData(conn: Connection, record: Record) = {
    // insert into $table (field1, field2, ...) values ( .... )
    val sb = StringBuilder.newBuilder.append("insert into ").append(record.table).append(" (")
    sb.append(record.rowData.cells.map(_.name).mkString(","))
    sb.append(") values (").append(Range(0, record.rowData.cells.size).map(_ => '?').mkString(",")).append(")")

    val stmt = conn.prepareStatement(sb.toString)
    try {
      record.rowData.cells.zipWithIndex.foreach { case (cell, idx) =>
        stmt.setObject(idx + 1, cell.getObject)
      }

      stmt.executeUpdate
    }
    finally {
      stmt.close
    }
  }

  private def getPrimaryKeys(conn: Connection, table: String): List[String] = {
    // TODO need to rewrite code for PK check
    // ensure we have the PK field
    val (catalog, tableName) = {
      val dot = table.indexOf(".")
      if(dot >= 0) (table.substring(0, dot), table.substring(dot+1))
      else ("", table)
    }
    val rs = conn.getMetaData.getPrimaryKeys(catalog, "", tableName.toUpperCase)
    val buffer = collection.mutable.ArrayBuffer[String]()
    while (rs.next) {
      val field = rs.getString("COLUMN_NAME")
      buffer += field
    }

    buffer.toList
  }

  private def updateData(conn: Connection, record: Record): Int = {

    val pk = getPrimaryKeys(conn, record.table)

    println("PK = " + pk)
    if(!pk.forall(record.rowData.cells.map(_.name.toUpperCase) contains _.toUpperCase)) 0
    else {

      val sb = StringBuilder.newBuilder.append("update ").append(record.table).append(" set ")

      val setPart = record.rowData.cells.filterNot(pk contains _.name).map { cell =>
        s"${cell.name}  = ? "
      }.mkString(",")
      sb.append(setPart)

      sb.append(" where ")
      val pkWhere = pk.map { field => s" $field = ? " }.mkString("and")
      sb.append(pkWhere)

      val stmt = conn.prepareStatement(sb.toString)
      try {
        var idx = 1
        record.rowData.cells.filterNot(pk contains _.name).foreach { cell =>
          stmt.setObject(idx, cell.getObject)
          idx += 1
        }
        pk.foreach { field =>
          stmt.setObject(idx, record.rowData.getObject(field))
          idx += 1
        }
        stmt.executeUpdate
      }
      finally {
        stmt.close
      }
    }
  }

  private def insertUpdate(conn: Connection, record: Record): Unit = {
      if( updateData(conn, record) == 0)
        insertData(conn, record)
  }

  // compare dataSource with a given dataset.
  // when row having PK field, it is compared via PK
  // otherwise it will match the first query "select * from table where field1=value and field2=value2 for
  //  all fields
  // this method should try to provide a print-friendly compare result

  // also compare will retuen a result which you can perform more test
  def compare(dataSource: DataSource, dataSet: DataSet): Boolean = {

    dataSource.withConnection { conn =>

      dataSet.records.foreach { record =>

        if(record.rowData == null){ // the table should have no data
          assert(conn.queryInt(s"select count(*) from ${record.table}") == 0)
        }
        else {
          val pk = getPrimaryKeys(conn, record.table)
          if (pk.forall(record.rowData.cells.map(_.name.toUpperCase) contains _.toUpperCase)) {
            compareRecordViaPK(conn, record, pk)
          }
          else {
            compareRecordViaFields(conn, record)
          }
        }
      }
    }
    true
  }

  private def compareRecordViaPK(conn: Connection, record: Record, pk: List[String]): Unit = {

    val sb = StringBuilder.newBuilder.append("select * from ").append(record.table).append(" where 1 = 1 ")
    pk.foreach { field =>
      sb.append(" and ").append(field).append( " = ? ")
    }
    val stmt = conn.prepareStatement(sb.toString)
    try {
      pk.zipWithIndex.foreach(x => stmt.setObject(x._2+1, record.rowData.getObject(x._1)))
      val rs = stmt.executeQuery()
      val rsMeta = rs.getMetaData

      if(rs.next) {
        val rsRow = Row.resultSetToRow(rsMeta, rs)
        record.rowData.cells.foreach { expected =>
          val real = rsRow.cell(expected.name)
          if(!compareCell(expected, rsRow.cell(expected.name)))
            throw new AssertionError(s"${record.table} not matched for ${record.rowData} for field ${expected.name}, expected: ${expected.value} real: ${real.value}")
        }
      }
      else {
        throw new AssertionError(s"${record.rowData} not exists")
      }
    }
    finally {
      stmt.close
    }

  }

  private def compareCell(expected: Row.Cell[_], real: Row.Cell[_]): Boolean = {
    if(expected.value == null) real.value == null
    else if(expected.sqltype == real.sqltype) expected.value == real.value
    else {
      expected match {
        case x: BooleanCell => real.getBoolean == x.getBoolean
        case x: Row.ByteCell => real.getLong == x.getLong
        case x: Row.IntegerCell => real.getLong == x.getLong
        case x: Row.ShortCell => real.getLong == x.getLong
        case x: Row.LongCell => real.getLong == x.getLong
        case x: Row.FloatCell => real.getDouble == x.getDouble
        case x: Row.DoubleCell => real.getDouble == x.getDouble
        case x: Row.StringCell => real.getString == x.getString
        case x: Row.BigDecimalCell => real.getBigDecimal == x.getBigDecimal
        case x: Row.DateCell => real.getDate == x.getDate
        case x: Row.TimeCell => real.getTime == x.getTime
        case x: Row.TimestampCell => real.getTimestamp == x.getTimestamp
        case x: Row.BytesCell => util.Arrays.equals( real.getBytes, x.getBytes )
        case x: Row.NullCell[_] => real.value == null
      }
    }
  }

  private def compareRecordViaFields(conn: Connection, record:Record): Unit = {
    val sb = StringBuilder.newBuilder.append("select * from ").append(record.table).append(" where 1=1 ")
    record.rowData.cells.foreach { cell =>
      if(cell.getObject == null)
        sb.append("and ").append(cell.name).append("is null")
      else
        sb.append("and ").append(cell.name).append("=? ")
    }

    val stmt = conn.prepareStatement(sb.toString)
    try {
      record.rowData.cells.filterNot(_.getObject == null).zipWithIndex.foreach { x =>
        stmt.setObject(x._2+1, x._1.getObject)
      }
      val rs = stmt.executeQuery()
      if(rs.next == false){
        throw new AssertionError(s"${record.rowData} not exists")
      }
    }
    finally {
      stmt.close
    }
  }

  implicit def xml2DataSet(root: Elem): DataSet = {
    assert(root.label == "dataset")

    val attrRE = """(@null|@date)""".r
    val REdate = """@date'(\d{4}-\d{2}-\d{2})'""".r
    val REtimestamp = """@timestamp'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})'""".r
    val REother = "@@(.*)"

    val records: List[Record] = root.child.toList.collect {
      case record: Elem =>
        val table = record.label

        val cells: List[Row.Cell[_]] = record.attributes.map { case x: MetaData =>
          x.value.text match {
            case "@null" => new Row.NullCell[String](x.key, Types.VARCHAR)
            case "@now" => new Row.TimestampCell(x.key, Types.TIMESTAMP, new java.sql.Timestamp(System.currentTimeMillis))
            case REdate(date) => new Row.DateCell(x.key, Types.DATE, java.sql.Date.valueOf(date))
            case REtimestamp(time) => new Row.TimestampCell(x.key, Types.TIMESTAMP, java.sql.Timestamp.valueOf(time))
            case str: String =>
              val unescape = if (str.startsWith("@@")) str.substring(1) else str
              new Row.StringCell(x.key, Types.VARCHAR, unescape)
          }
        } toList

        Record(table, if (cells == Nil) null else new Row(cells))
    }

    new DataSet(records)

  }

}


class DataSet(val records: List[DataSet.Record]) {

  def save(datasource: DataSource, syncMode: DataSetSyncMode = INSERT_UPDATE) =
    DataSet.save(datasource, this, syncMode)

  def compareTo(datasource: DataSource): Boolean = DataSet.compare(datasource, this)


  def +(other: DataSet) = new DataSet(this.records ++ other.records)

  def toXML: NodeSeq = records.map { record =>
    val attributes = record.rowData.cells.foldRight(Null: MetaData) { (cell: Row.Cell[_], prev: MetaData) =>
      Attribute(null, cell.name, cell.getString, prev)
    }
    <some/>.copy(label = record.table).copy(attributes = attributes)
  }

  def printXML() = toXML.foreach { node =>
    val printer = new PrettyPrinter(120,4)
    val sb = new StringBuilder
    printer.format(node, sb)
    println(sb.toString)
  }

}
