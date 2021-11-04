//package demo_s
//
//import java.util.Date
//import scala.collection.JavaConverters._
//
//case class Order
//(
//  orderId: Long,
//  amount: BigDecimal,
//  items: List[OrderItem],
//  createdAt: Date,
//)
//
//object Order {
//  implicit def j2s(jInst: demo.Order): demo_s.Order = demo_s.Order(
//    orderId = jInst.getOrderId,
//    amount = jInst.getAmount,
//    items = jInst.getItems.asScala.toList.map(implicitly[demo.OrderItem=>demo_s.OrderItem]),
//    createdAt = jInst.getCreatedAt)
//
//  implicit def s2j(sInst: demo_s.Order): demo.Order = {
//    val result = new demo.Order
//    result.setOrderId(sInst.orderId)
//    result.setAmount(sInst.amount.underlying())
//    result.setItems(sInst.items.map(implicitly[demo_s.OrderItem=>demo.OrderItem]).asJava)
//    result.setCreatedAt(sInst.createdAt)
//    result
//  }
//}
//
//case class OrderItem
//(
//  orderItemId: Long,
//  skuId: Long,
//  price: BigDecimal,
//  count: Int,
//  totalAmount: BigDecimal
//)
//
//object OrderItem{
//  implicit def j2s(jInst: demo.OrderItem): demo_s.OrderItem = demo_s.OrderItem(
//    orderItemId = jInst.getOrderItemId,
//    skuId = jInst.getSkuId,
//    price = jInst.getPrice,
//    count = jInst.getCount,
//    totalAmount = jInst.getTotalAmount
//  )
//
//  implicit def s2j(sInst: demo_s.OrderItem): demo.OrderItem = {
//    val result = new demo.OrderItem();
//    result.setOrderItemId(sInst.orderItemId)
//    result.setSkuId(sInst.skuId)
//    result.setPrice(sInst.price.underlying)
//    result.setCount(sInst.count)
//    result.setTotalAmount(sInst.totalAmount.underlying)
//    result
//  }
//}
//
//trait OrderService {
//  def createOrder(order: Order): Int
//}
//
//object OrderService {
//  implicit def j2s(jInst: demo.OrderService): demo_s.OrderService = new demo_s.OrderService {
//    override def createOrder(order: demo_s.Order): Int =
//      jInst.createOrder(order)
//  }
//
//  implicit def s2j(sInst: demo_s.OrderService): demo.OrderService = new demo.OrderService {
//    override def createOrder(order: demo.Order): Int =
//      sInst.createOrder(order)
//  }
//}
//
