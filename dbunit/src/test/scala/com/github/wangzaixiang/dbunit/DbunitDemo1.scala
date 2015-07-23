package com.github.wangzaixiang.dbunit

import javax.sql.DataSource
import wangzx.scala_commons.sql._

import scala.xml.{NodeSeq, PrettyPrinter, Node}

import spray.json._

/**
 * Created by wangzx on 15/7/21.
 */
object DbunitDemo1 {

  val datasource = {
    val ds = new org.h2.jdbcx.JdbcDataSource
    ds.setURL("jdbc:h2:./db/test")
    ds.setUser("sa")
    ds.setPassword("")
    ds
  }

  def main(args: Array[String]): Unit = {
    datasource executeUpdate
      """
        drop table if exists Users;

        create table Users( name varchar(20), phone varchar(20), email varchar(50), birthday date, primary key(name));

        drop table if exists books;

        create table books( id int auto_increment primary key, name varchar(64) not null, author varchar(64) not null,
          price decimal(13,2) not null);

        -- insert into Users values('wangzx', '18620208972', 'wangzaixiang@gmail.com', '1973-03-22');
        -- insert into Users values('rainbow', '18665579114', 'rainbow@gmail.com', '1975-03-22');
      """

    // val dataset: NodeSeq = DataSet.dumpXml("users", datasource.rows[Row]("select * from Users"))

    // dataset.foreach(x => println(prettyPrint(x)))

    val dataset: DataSet =
      <dataset>
        <users name="wangzx" phone="18620208972" email="wangzx@gmail.com" birthday="1973-03-22"/>
        <users name="rainbow" phone="18665579114" email="rainbow@gmail.com" birthday="1975-03-22"/>
      </dataset>

    dataset.save(datasource)

    datasource.rows[Row]("select * from users").foreach(println)
    datasource.rows[Row]("select * from books").foreach(println)

    datasource executeUpdate
      """
        update users set email = '949631531@qq.com' where name = 'wangzx';
        insert into books values(null, '整理的艺术(一）', '小山龙介', 28.0);
      """

    datasource.rows[Row]("select * from books").foreach(println)

    val expected: DataSet =
      <dataset>
        <users name="wangzx" email="949631531@qq.com"/>
        <books name="整理的艺术(一）" price="28"/>
      </dataset>

    assert( expected.compareTo(datasource) )

  }

  def prettyPrint(xml: Node): String = {
    val sb = new StringBuilder
    new PrettyPrinter(120, 4).format(xml, sb)
    sb.toString
  }

  def callService(service: String, request: JsValue): JsValue = ???

  def step[T](label: String)(value: =>T) = value

  def testConcepts(): Unit = {

    step("prepare data") {
//      commonDataSet.save(datasource)
      val prepare =
        <dataset>
          <users/>
          <users name="wangzhx" birthday="2001-08-08"/>
          <orders designation=""></orders>
          <order_item designation=""></order_item>
        </dataset>

      prepare.save(datasource)
    }

    step("call service") {
      val request =
        json"""{
            header: { operatorId: 0, customerId: 0, callFrom: 'web', }
            args: [ 'PVC' ]
        }"""
      val response = callService("OrderExtraService.getRankingOrderItem", request)

      val expJson =
        json"""
        result: [
          { category: 'PVC', application: '2', designation: 'SG-5', manufacturerId: 0, manufacturerName: '内蒙君正（新）', weight: 1409.0 },
          { category: 'PVC', application: '2', designation: 'SG-5', manufacturerId: 0, manufacturerName: '内蒙君正（新）', weight: 1409.0 }
        ]
      """

      // using json to retreive result and perform compare
      response match {
        case `expJson` => // OK
      }
    }

    step("compare database") {
      val expected =
        <dataset>
          <users name="wangzx" email="949631531@qq.com" />
        </dataset>
      assert(expected.compareTo(datasource))
    }
  }


}
