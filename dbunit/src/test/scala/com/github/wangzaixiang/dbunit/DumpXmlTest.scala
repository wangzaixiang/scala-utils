package com.github.wangzaixiang.dbunit

import org.junit.Test
import wangzx.scala_commons.sql._

/**
  * Created by wangzx on 15/12/9.
  */
class DumpXmlTest {


  @Test
  def test1(): Unit ={

    val ds = DemoDB.datasource
    val rows = ds.rows[Row]("select * from users")

    DataSet("users", rows).printXML()

  }

  @Test
  def test2(): Unit = {

    val ds = DemoDB.datasource
    val users = ds.rows[Row]("select * from users")
    val books = ds.rows[Row]("select * from books")

    val dataset = DataSet("users", users) + DataSet("books", books)
    dataset.printXML()


  }

}
