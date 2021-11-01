# Bean 2 Case Class Util

b2c(Bean 2 Case) 是一个将Java Bean 转换为 Case Class 的源代码转换工具。它扫描 Java 源文件中的 JavaBean，生成对应的 Scala Case Class。

- 为什么需要 b2c 工具

  虽然scala是可以互操作java的，但简单的混合scala和java并不是一个很好的体验。JavaBean的mutable模式，很容易打破函数式编程中所倡导的immutable理念，劣币驱逐良币，
  最终会导致混合后的代码回归到java风格。
  
  b2c 工具会尝试对 JavaBeans 映射到 Case Class，并生成二者的转换代码。当服务接口使用 JavaBeans 风格时，我们可以将其转换为 Case Class风格。
  
- b2c 能带来什么？
  - producer侧，使用 immutable 风格来实现(mutable风格的)服务接口，与 Java代码无缝隙衔接。例如 dubble 服务接口时mutable风格的，通过b2c，我们可以使用 immutable 风格来实现服务。
  - consumer侧，使用 immutable 风格来调用 mutable 风格的服务接口。

# Use Case

```java
package java_version; 
  
class Order {
  int orderId;
  int customerId;
  List<OrderItem> items;
  
  // getters and setters
}

class OrderItem {
  int skuId;
  int count;
  BigDecimal price;
  BigDecimal total;
  
  // getters and setters
}

interface OrderService {
   int createOrder(Order order);
}

```

```scala
package scala_version

case class Order(orderId: Int, customerId: Int, items: List[OrderItem]) {
  def toJava: java_version.Order = ...
}
case class OrderItem(skuId:Int, count: Int, price: BigDecimal, total: BigDecimal) {
  def toJava: java_version.OrderItem = ...
}
implicit class OrderExtension(j: java_version.Order){
  def toScala: Order = ...
}
implict class OrderItemExtension(j: java_version.OrderItem){
  def toScala: OrderItem = ...

trait OrderService {
  def createOrder(order: Order): Int
}

class JavaOrderService(s: OrderService) extends java_version.OrderService {
  def createOrder(order: java_version.Order) = s.createOrder( order.toScala )
}

class ScalaOrderService(j: java_version.OrderService) extends OrderService {
  def createOrder(order: Order) = j.createOrder(order.toJava)
}

```

这个例子描述了 b2c 的使用模式。

# Usage
1. 生成代码。需要考虑到生成的 Scala 代码中包含字段注释等信息，以方便在 IDE 中进行源代码阅读。
2. 使用 Scala 实现服务后，使用 JavaOrderService 版本暴露给 Java consumer来使用。
3. 如果要调用 Java版本的服务接口，可以使用 ScalaOrderService来包装 Java provider来使用。
4. 项目，可以改写为 Java/Scala 混合编译模式的项目，新生成的scala代码在一个独立的源代码目录，和一个独立的package下。（maven混合模式，或者sbt混合模式）

# Features
1. optional/default value 设置：可以在Java版本中使用 annotation，或者在独立的配置文件中设置。具有optional的字段，在Case Class中，可以携带缺省值。 
2. 类型替换。例如，使用 Optional<T> 来替代可为空的字段。使用 scala.BigDecimal来替换 java.util.BigDecimal等。
