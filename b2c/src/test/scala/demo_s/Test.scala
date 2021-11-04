package demo_s

import bean2case._

import java.io.File

object Test {

  def main(args: Array[String]): Unit = {
    val dir = new File("./b2c/src/test/scala")

    val context = new CodeContext

    new Bean2Case(context, classOf[demo.OrderItem]).generate(dir)
    new Bean2Case(context, classOf[demo.Order]).generate(dir)

    new ServiceCompanion(context, classOf[demo.OrderService]).generate(dir)

  }

}
