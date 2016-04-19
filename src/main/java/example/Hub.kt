package example

import com.github.rinde.rinsim.core.model.comm.CommDevice
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.pdp.Depot
import com.github.rinde.rinsim.core.model.pdp.PDPModel
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList

/**
 * Created by victo on 19/04/2016.
 */

class Hub(location: Point) : Depot(location), TickListener, CommUser {


    var device: CommDevice? = null

    var hasClientContract = false
    var pickedUp = false
    var messageBroadcast = false

    private var order: Order? = null
        get() = getOrders()

    private fun getOrders(): Order? {
        var rm = roadModel
        var pdp = pdpModel
        val allOrders = rm.getObjectsOfType(Order::class.java).iterator()
        var hubOrder : Order? = null
        while(allOrders.hasNext()) {
            val corder = allOrders.next()
            if (corder.origin === this) {
                hubOrder = corder
            }
        }
        return hubOrder
    }

    private var currentOrder : PackageType? = null

    override fun initRoadPDP(pRoadModel: RoadModel?, pPdpModel: PDPModel?) {
    }

    override fun getPosition(): Optional<Point> {
        val rm = roadModel
        if (rm.containsObject(this)) {
            return Optional.of(rm.getPosition(this))
        }
        return Optional.absent<Point>()

    }

    override fun setCommDevice(builder: CommDeviceBuilder) {
        device = builder.setReliability(1.0).build()
    }


    override fun tick(timeLapse: TimeLapse) {

        if (!hasClientContract) {
            val messages = device?.unreadMessages
            // bid on incoming contract proposals
            for (message in messages!!) {
                if (message.contents is WinningClientBidMessage) {
                    currentOrder = (message.contents as WinningClientBidMessage).order
                    hasClientContract = true
                }
                else if (message.contents is ClientOfferMessage) {
                    val wantedPackage = (message.contents as ClientOfferMessage).order
                    if (wantedPackage === order!!.content) {
                        val distance = Point.distance(message.sender.position.get(), this.position.get())
                        device?.send(BiddingMessage(distance, order!!), message.sender)
                    }
                }
            }
        }
        else {
            if (!pickedUp && !messageBroadcast) {
                device?.broadcast(HubOfferMessage(order))
                messageBroadcast = true
            }
            if (messageBroadcast) {
                var bestBid = Integer.MAX_VALUE.toInt().toDouble()
                var bestVehicle: Drone? = null
                val messages = device?.unreadMessages ?: ImmutableList.of()
                for (i in messages.indices) {
                    val message = messages[i]
                    if (message.contents is BiddingMessage) {
                        val contents = message.contents as BiddingMessage

                        val bid = contents.bid
                        if (bid < bestBid) {
                            bestBid = bid
                            bestVehicle = message.sender as Drone
                        }
                    }
                }
                if (bestVehicle != null) {
                    device?.send(WinningBidMessage(order), bestVehicle)
                    val messages2 = device?.unreadMessages ?: ImmutableList.of()
                    for (i in messages2.indices) {
                        if (messages2[i].contents === Messages.I_CHOOSE_YOU) {
                            pickedUp = true
                            break
                        }
                    }
                }
            } else {
                device?.unreadMessages
            }
        }
    }

    override fun afterTick(timeLapse: TimeLapse) {
        // TODO Auto-generated method stub

    }

}
