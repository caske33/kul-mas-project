package example

import com.github.rinde.rinsim.core.model.comm.MessageContents

class DeclareOrder(val order: Order) : MessageContents
class BidOnOrder(val order: Order, val value: Double) : MessageContents
class AcceptOrder(val order: Order) : MessageContents
class ConfirmOrder(val order: Order) : MessageContents
class CancelOrder(val order: Order) : MessageContents

