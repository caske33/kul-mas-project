package example

import com.github.rinde.rinsim.core.model.comm.MessageContents

class DeclareOrder(val order: Order) : MessageContents

class BidOnOrder(val bid: Bid) : MessageContents

class AcceptOrder(val bid: Bid) : MessageContents

class ConfirmOrder(val acceptOrder: AcceptOrder) : MessageContents{
    val order: Order
        get() = acceptOrder.bid.order
}
class CancelOrder(val acceptOrder: AcceptOrder) : MessageContents{
    val order: Order
        get() = acceptOrder.bid.order
}

