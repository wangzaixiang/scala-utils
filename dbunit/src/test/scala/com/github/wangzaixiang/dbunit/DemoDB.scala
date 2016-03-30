package com.github.wangzaixiang.dbunit

import wangzx.scala_commons.sql._

import scala.xml.{Node, PrettyPrinter, NodeSeq}

/**
  * Created by wangzx on 15/12/9.
  */
object DemoDB {

  val datasource = {
    val ds = new org.h2.jdbcx.JdbcDataSource
    ds.setURL("jdbc:h2:./db/test")
    ds.setUser("sa")
    ds.setPassword("")

    ds executeUpdate
      """
        drop table if exists Users;

        create table Users( name varchar(20), phone varchar(20), email varchar(50), birthday date, primary key(name));

        drop table if exists books;

        create table books( id int auto_increment primary key, name varchar(64) not null, author varchar(64) not null,
          price decimal(13,2) not null);

        insert into Users values('wangzx', '18620208972', 'wangzaixiang@gmail.com', '1973-03-22');
        insert into Users values('rainbow', '18665579114', 'rainbow@gmail.com', '1975-03-22');

        insert into books values(1, 'Coding Scala', 'wangzx', 39.99);
      """

    ds
  }

  def prettyPrint(xml: Node): String = {

    val printer = new PrettyPrinter(120,4)
    val sb = new StringBuilder
    printer.format(xml, sb)

    sb.toString
  }

}
