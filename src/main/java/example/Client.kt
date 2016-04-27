package example

import com.github.rinde.rinsim.core.model.comm.CommDevice
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.pdp.Depot
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList

class Client(location: Point) : Depot(location), TickListener, CommUser {

    private var hasContract : Boolean = false
    private var messageBroadcast : Boolean = false
    private var device : CommDevice? = null

    override fun afterTick(timeLapse: TimeLapse?) {
        //
    }

    override fun tick(timeLapse: TimeLapse?) {
        /*
        if (!hasContract && !messageBroadcast) {
            device?.broadcast(ClientOfferMessage(PackageType.IPOD))
            messageBroadcast = true
        }
        if (!hasContract && messageBroadcast) {
            var bestBid = Integer.MAX_VALUE.toInt().toDouble()
            var bestVehicle: Depot? = null
            val messages = device?.unreadMessages ?: ImmutableList.of()
            for (i in messages.indices) {
                val message = messages[i]
                if (message.contents is DroneBiddingMessage && message.sender is Depot) {
                    val contents = message.contents as DroneBiddingMessage

                    val bid = contents.bid
                    if (bid < bestBid) {
                        bestBid = bid
                        bestVehicle = message.sender as Depot
                    }
                }
            }
            if (bestVehicle != null) {
                device?.send(WinningClientBidMessage(PackageType.IPOD), bestVehicle)
                //val messages2 = device?.unreadMessages ?: ImmutableList.of()
                //for (i in messages.indices) {
                //    if (messages[i].contents === Messages.I_CHOOSE_YOU) {
                hasContract = true
                println("Contract for client " + this.hashCode() + " awarded to " + bestVehicle.hashCode())
                //        break
                //    }
                //}
            }
        } else {
            device?.unreadMessages
        }
        */
    }

    override fun setCommDevice(builder: CommDeviceBuilder?) {
        device = builder!!.setReliability(1.0).build()
    }

    override fun getPosition(): Optional<Point>? {
        val rm = roadModel
        if (rm.containsObject(this)) {
            return Optional.of(rm.getPosition(this))
        }
        return Optional.absent<Point>()
    }

    val realPosition: Point
        get() = position?.get()!!

}
