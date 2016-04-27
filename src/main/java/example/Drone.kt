package example

import com.github.rinde.rinsim.core.model.comm.CommDevice
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.comm.Message
import com.github.rinde.rinsim.core.model.pdp.Vehicle
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO
import com.github.rinde.rinsim.core.model.road.MovingRoadUser
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional

class Drone(var position: Point) :
        Vehicle(
                VehicleDTO.builder()
                        .capacity(1)
                        .startPosition(position)
                        .speed(DRONE_SPEED)
                        .build()
        ), MovingRoadUser, TickListener, CommUser {
    // the MovingRoadUser interface indicates that this class can move on a
    // RoadModel. The TickListener interface indicates that this class wants
    // to keep track of time. The RandomUser interface indicates that this class
    // wants to get access to a random generator

    private var currentOrder: Order? = null
    private var device: CommDevice? = null
    private var state: DroneState = DroneState.IDLE
    private var batteryLevel: Double = 1.0

    override fun afterTick(timeLapse: TimeLapse?) {
        // we don't need this in this example. This method is called after
        // all TickListener#tick() calls, hence the name.
    }

    override fun tickImpl(time: TimeLapse) {
        if (!time.hasTimeLeft()) {
            return
        }

        val messages = device?.unreadMessages

        handleMessages(time, messages?.toMutableList()?.toList()!!)

        /*if (!isDelivering && !hasContract) {
            // bid on incoming contract proposals
            for (message in messages!!) {
                if (message.contents is WinningBidMessage) {
                    if (dynamic && hasContract) {
                        device?.send(CancelMessage(), currentOrder!!.client)
                    }
                    currentOrder = (message.contents as WinningBidMessage).order
                    hasContract = true
                }
                else if (message.contents is HubOfferMessage) {
                    val hub = message.sender as Hub
                    val hubPos = hub.position.get()
                    val order = (message.contents as HubOfferMessage).order
                    val distance = Point.distance(hubPos, this.getPosition().get())
                    val currentDistance = Point.distance(order!!.pickupLocation, this.getPosition().get()) + Point.distance(order.pickupLocation, order.deliveryLocation)
                    device?.send(DroneBiddingMessage(currentDistance, order), hub)
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
        }*/
    }

    fun handleMessages(time: TimeLapse, messages: List<Message>) {
        // DeclareOrder
        if(state.canBid()){
            messages.filter { message -> message.contents is DeclareOrder }.forEach { message ->
                val order: Order = (message.contents as DeclareOrder).order

                val warehouse: Warehouse? = getCheapestWarehouse(order, time)
                if(warehouse != null) {
                    val cost = -order.price + estimatedCostWarehouse(warehouse, order.type, message.sender as Client)
                    if(cost < order.fine) // Otherwise beter om order te laten vervallen
                        device?.send(BidOnOrder(order, cost), message.sender)
                }
            }
        }

        // AcceptOrder
        val acceptOrderMessages = messages.filter { message -> message.contents is AcceptOrder }
        if(acceptOrderMessages.size > 0){

            val winningOrder: Message = acceptOrderMessages.minBy { message -> (message.contents as AcceptOrder).bid }!!
            device?.send(ConfirmOrder(winningOrder.contents as AcceptOrder), winningOrder.sender)

            acceptOrderMessages.filter { message -> message != winningOrder }.forEach { message ->
                device?.send(CancelOrder(message.contents as AcceptOrder), message.sender)
            }
        }
    }

    fun getCheapestWarehouse(order: Order, time: TimeLapse): Warehouse? {
        return roadModel.getObjectsOfType(Warehouse::class.java).filter { warehouse ->
            val distance = Point.distance(position, warehouse.position) + Point.distance(warehouse.position, order.client.position)
            val traveltime = distance / DRONE_SPEED
            time.startTime + traveltime < order.endTime
        }.minBy { warehouse ->
            estimatedCostWarehouse(warehouse, order.type, order.client)
        }
    }

    fun estimatedCostWarehouse(warehouse: Warehouse, type: PackageType, client: Client): Double
        = warehouse.getPriceFor(type) +
        estimatedCostFailureOnTrajectory(position, warehouse.position, batteryLevel) +
        estimatedCostFailureOnTrajectory(warehouse.position, client.position, batteryLevel - batteryDrainTrajectory(position, warehouse.position), type.marketPrice) +
        costForEnergyOnTrajectory(position, warehouse.position) +
        costForEnergyOnTrajectory(warehouse.position, client.position)

    fun costForEnergyOnTrajectory(p1: Point, p2: Point)
        = Point.distance(p1, p2) * COST_FOR_ENERGY_PER_DISTANCE_UNIT

    fun estimatedCostFailureOnTrajectory(p1: Point, p2: Point, startBatteryLevel: Double, priceContentsDrone: Double = 0.0): Double {
        val endBatteryLevel = startBatteryLevel - batteryDrainTrajectory(p1, p2)
        if(!BatteryState.isSameState(startBatteryLevel, endBatteryLevel)){
            val stateChangeLevel = BatteryState.values().map { it.upperBound }.filter { it < startBatteryLevel }.first()
            val middlePointDistance = (startBatteryLevel-stateChangeLevel)*DISTANCE_PER_PERCENTAGE_BATTERY_DRAIN
            val middlePoint = middlePoint(p1, p2, middlePointDistance)
            return estimatedCostFailureOnTrajectory(p1, middlePoint, startBatteryLevel) + estimatedCostFailureOnTrajectory(middlePoint, p2, stateChangeLevel)
        }

        return Point.distance(p1, p2) / BatteryState.stateFromLevel(startBatteryLevel).failureLambda * (PRICE_DRONE + priceContentsDrone)
    }

    private fun middlePoint(p1: Point, p2: Point, distance: Double): Point{
        val totalDistance = Point.distance(p1, p2)
        val percentage = distance / totalDistance

        // TODO: fix after release of https://github.com/rinde/RinSim/pull/33
        return Point.diff(Point.divide(Point.diff(p2, p1), 1 / percentage), Point.divide(p1, -1.0))
    }

    fun batteryDrainTrajectory(p1: Point, p2: Point): Double
        = Point.distance(p1, p2) / DISTANCE_PER_PERCENTAGE_BATTERY_DRAIN

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
}

enum class DroneState(private val canBid : Boolean) {
    IDLE(true),
    PICKING_UP(false),
    CHARGING(true),
    DELIVERING(false),
    CHARGING_FOR_CONTRACT(false);

    fun canBid() = canBid
}

enum class BatteryState(val lowerBound: Double, val upperBound: Double, val failureLambda: Int) {
    CRITICAL(-1e-15, 0.1, LAMBDA_CRITICAL),
    LOW(0.1, 0.2, LAMBDA_LOW),
    NORMAL(0.2, 1.0, LAMBDA_NORMAL);

    companion object {
        fun isSameState(level1: Double, level2: Double)
                = stateFromLevel(level1) == stateFromLevel(level2)

        fun stateFromLevel(level: Double): BatteryState {
            return BatteryState.values().filter { state ->
                state.lowerBound < level && state.upperBound >= level;
            }.first()
        }
    }
}
