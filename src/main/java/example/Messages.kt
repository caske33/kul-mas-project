package example

import com.github.rinde.rinsim.core.model.comm.MessageContents

class DeclareOrder(val order: Order) : MessageContents

class BidOnOrder(val order: Order, val bid: Double) : MessageContents

class AcceptOrder(val order: Order, val bid: Double) : MessageContents

class ConfirmOrder(val acceptOrder: AcceptOrder) : MessageContents{
    val order: Order
        get() = acceptOrder.order
}
class CancelOrder(val acceptOrder: AcceptOrder) : MessageContents{
    val order: Order
        get() = acceptOrder.order
}

