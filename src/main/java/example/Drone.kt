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

class Drone(position: Point) :
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

    private var currentBid: Bid? = null
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

        when(state){
            DroneState.IDLE -> moveToClosestWarehouse(time)
            DroneState.CHARGING -> charge(time)
            DroneState.CHARGING_FOR_CONTRACT -> chargeUntilMove(time)
            DroneState.PICKING_UP -> moveToWarehouse(time, currentBid!!.warehouse)
            DroneState.DELIVERING -> moveToClient(time)
        }

        //TODO: move
        //TODO: cost update
        //TODO: deliver package?
        //TODO: more state changes!
        //TODO: crashes! (statistically correct!)
        //TODO: pay energy + battery drain for moving
    }
    fun moveToClosestWarehouse(time: TimeLapse) {
        val closest = roadModel.getObjectsOfType(Warehouse::class.java).minBy { warehouse ->
            Point.distance(realPosition, warehouse.position)
        }!!
        if(realPosition.equals(closest.position)){
            state = DroneState.CHARGING
            charge(time)
        } else {
            roadModel.moveTo(this, closest, time)
        }
    }
    fun moveToWarehouse(time: TimeLapse, warehouse: Warehouse) {
        if(realPosition.equals(warehouse.position)){
            state = DroneState.DELIVERING
            moveToClient(time)
            //TODO pay cost package
        } else {
            roadModel.moveTo(this, warehouse, time)
        }
    }
    fun charge(time: TimeLapse) {
        batteryLevel += time.timeLeft * BATTERY_CHARGING_RATE
        if(batteryLevel > 1.0)
            batteryLevel = 1.0
    }
    fun moveToClient(time: TimeLapse) {
        val client = currentBid!!.order.client
        if(realPosition.equals(client.position)){
            state = DroneState.IDLE
            //TODO get paid
            moveToClosestWarehouse(time)
        } else {
            roadModel.moveTo(this, client, time)
        }
    }
    fun chargeUntilMove(time: TimeLapse) {
        //TODO charge, zo lang ge nog niet moet vertrekken
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
                        device?.send(BidOnOrder(Bid(order, cost, warehouse)), message.sender)
                }
            }
        }

        // AcceptOrder
        val acceptOrderMessages = messages.filter { message -> message.contents is AcceptOrder }
        if(acceptOrderMessages.size > 0){

            // accept order with lowest (estimated) cost
            val winningOrder: Message = acceptOrderMessages.minBy { message -> (message.contents as AcceptOrder).bid.bidValue }!!
            device?.send(ConfirmOrder(winningOrder.contents as AcceptOrder), winningOrder.sender)
            state = DroneState.PICKING_UP
            currentBid = (winningOrder.contents as AcceptOrder).bid

            // cancel other orders
            acceptOrderMessages.filter { message -> message != winningOrder }.forEach { message ->
                device?.send(CancelOrder(message.contents as AcceptOrder), message.sender)
            }
        }
    }

    fun getCheapestWarehouse(order: Order, time: TimeLapse): Warehouse? {
        return roadModel.getObjectsOfType(Warehouse::class.java).filter { warehouse ->
            val distance = Point.distance(realPosition, warehouse.position) + Point.distance(warehouse.position, order.client.position)
            val traveltime = distance / DRONE_SPEED
            time.startTime + traveltime < order.endTime
        }.minBy { warehouse ->
            estimatedCostWarehouse(warehouse, order.type, order.client)
        }
    }

    fun estimatedCostWarehouse(warehouse: Warehouse, type: PackageType, client: Client): Double
        = warehouse.getPriceFor(type) +
        estimatedCostFailureOnTrajectory(realPosition, warehouse.position, batteryLevel) +
        estimatedCostFailureOnTrajectory(warehouse.position, client.position, batteryLevel - batteryDrainTrajectory(realPosition, warehouse.position), type.marketPrice) +
        costForEnergyOnTrajectory(realPosition, warehouse.position) +
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
    val realPosition: Point
        get() = position.get()!!

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
