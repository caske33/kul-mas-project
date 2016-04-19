package example

import com.github.rinde.rinsim.core.model.comm.CommDevice
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.pdp.Vehicle
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO
import com.github.rinde.rinsim.core.model.road.MovingRoadUser
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional

class Drone(var position: Point) : Vehicle(VehicleDTO.builder().capacity(1).startPosition(position).speed(Drone.SPEED).build()), MovingRoadUser, TickListener, CommUser {
    // the MovingRoadUser interface indicates that this class can move on a
    // RoadModel. The TickListener interface indicates that this class wants
    // to keep track of time. The RandomUser interface indicates that this class
    // wants to get access to a random generator

    private var currentOrder: Order? = null
    private var hasContract : Boolean = false
    private var isDelivering : Boolean = false
    private var dynamic = false
    private var device : CommDevice? = null

    override fun afterTick(timeLapse: TimeLapse?) {
        // we don't need this in this example. This method is called after
        // all TickListener#tick() calls, hence the name.
    }

    override fun tickImpl(time: TimeLapse) {
        val rm = roadModel
        val pm = pdpModel

        if (!time.hasTimeLeft()) {
            return
        }

        val messages = device?.unreadMessages

        if (!isDelivering && !hasContract) {
            // bid on incoming contract proposals
            for (message in messages!!) {
                if (message.contents is ContractNetExample.WinningBidMessage) {
                    if (dynamic && hasContract) {
                        device?.send(ContractNetExample.CancelMessage(), currentOrder!!.origin)
                    }
                    currentOrder = (message.contents as ContractNetExample.WinningBidMessage).order
                    hasContract = true
                }
                else if (message.contents is ContractNetExample.HubOfferMessage) {
                    val hub = message.sender as ContractNetExample.Hub
                    val hubPos = hub.position.get()
                    val order = (message.contents as ContractNetExample.HubOfferMessage).order
                    val distance = Point.distance(hubPos, this.getPosition().get())
                    val currentDistance = Point.distance(order!!.pickupLocation, this.getPosition().get()) + Point.distance(order.pickupLocation, order.deliveryLocation)
                    device?.send(ContractNetExample.BiddingMessage(currentDistance, order), hub)
                }
            }
        }

        else {
            val inCargo = pm.containerContains(this, currentOrder)
            // sanity check: if it is not in our cargo AND it is also not on the
            // RoadModel, we cannot go to curr anymore.
            if (!inCargo && !rm.containsObject(currentOrder)) {
                currentOrder = null
            } else if (inCargo) {
                // if it is in cargo, go to its destination
                rm.moveTo(this, currentOrder!!.deliveryLocation, time)
                if (rm.getPosition(this) == currentOrder!!.deliveryLocation) {
                    // deliver when we arrive
                    pm.deliver(this, currentOrder!!, time)
                    currentOrder!!.isDelivered = true
                }
            } else {
                // it is still available, go there as fast as possible
                rm.moveTo(this, currentOrder!!.pickupLocation, time)
                if (rm.equalPosition(this, currentOrder!!)) {
                    // pickup customer
                    pm.pickup(this, currentOrder!!, time)
                }
            }
        }
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

    companion object {
        private val SPEED = 1000.0
    }


}