package example

import com.github.rinde.rinsim.core.model.comm.MessageContents

class DeclareOrder(val order: Order) : MessageContents

class BidOnOrder(val bid: Bid) : MessageContents

class AcceptOrder(val bid: Bid) : MessageContents

class ConfirmOrder(val bid: Bid) : MessageContents{
    val order: Order
        get() = bid.order
}
class CancelOrder(val bid: Bid) : MessageContents{
    val order: Order
        get() = bid.order
}

class GotBetterOffer(val orderThatIsLost: Order) : MessageContents

class DroneCrashMessage() : MessageContents
