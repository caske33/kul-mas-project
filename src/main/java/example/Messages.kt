package example

import com.github.rinde.rinsim.core.model.comm.MessageContents

class CallForProposal(val order: Order) : MessageContents

class Propose(val bid: Bid) : MessageContents
class Refuse(val order: Order, val refuseReason: RefuseReason, val minimumBusyTime: Long = 0) : MessageContents

enum class RefuseReason {
    BUSY,
    INELIGIBLE,
    LOW_RANKING
}

class RejectProposal(val bid: Bid) : MessageContents
class AcceptProposal(val bid: Bid) : MessageContents

class Agree(val bid: Bid) : MessageContents
abstract class Cancel(val bid: Bid) : MessageContents
class Disagree(bid: Bid) : Cancel(bid)
class CancelContract(bid: Bid) : Cancel(bid)

class GotBetterOffer(val orderThatIsLost: Order) : MessageContents

class Failure() : MessageContents

class InformDone(val order: Order, val deliveryTime: Long) : MessageContents
class InformPickedUp(val order: Order, val pickupTime: Long) : MessageContents
