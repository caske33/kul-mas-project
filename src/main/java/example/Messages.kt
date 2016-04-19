package example

import com.github.rinde.rinsim.core.model.comm.MessageContents

enum class Messages : MessageContents {
    PICK_ME_UP, I_CHOOSE_YOU
}

class BiddingMessage(var bid: Double, var order: Order?) : MessageContents

class CancelMessage() : MessageContents

class WinningBidMessage(var order: Order?) : MessageContents

class ClientOfferMessage(var order: PackageType?) : MessageContents

class WinningClientBidMessage(var order : PackageType?) : MessageContents

class HubOfferMessage(var order: Order?) : MessageContents
