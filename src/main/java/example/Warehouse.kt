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
import org.apache.commons.math3.random.RandomGenerator

class Warehouse(val position: Point, val rng: RandomGenerator) : Depot(position), TickListener, CommUser {

    var device: CommDevice? = null

    var hasClientContract = false
    var pickedUp = false
    var messageBroadcast = false

    val prices: Map<PackageType, Double>

    init {
        prices = PackageType.values().associate { type ->
            val price = type.marketPrice * (1 + rng.nextDouble()*0.4-0.2) // +/- 20% of market price
            Pair(type, price)
        }
    }

    override fun initRoadPDP(pRoadModel: RoadModel?, pPdpModel: PDPModel?) {
    }

    override fun getPosition(): Optional<Point> {
        return Optional.of(position)
    }

    override fun setCommDevice(builder: CommDeviceBuilder) {
        device = builder.setReliability(1.0).build()
    }

    fun getPriceFor(type: PackageType): Double
        = prices.get(type)!!

    override fun tick(timeLapse: TimeLapse) {

        /*
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
                        device?.send(DroneBiddingMessage(distance, order!!), message.sender)
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
                    if (message.contents is DroneBiddingMessage) {
                        val contents = message.contents as DroneBiddingMessage

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
        }*/
    }

    override fun afterTick(timeLapse: TimeLapse) {
        // TODO Auto-generated method stub

    }
}
