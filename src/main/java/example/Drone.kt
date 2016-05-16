package example

import com.github.rinde.rinsim.core.model.comm.CommDevice
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.comm.Message
import com.github.rinde.rinsim.core.model.pdp.Vehicle
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO
import com.github.rinde.rinsim.core.model.road.MovingRoadUser
import com.github.rinde.rinsim.core.model.road.RoadUser
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional
import org.apache.commons.math3.random.RandomGenerator

//TODO: Dynamisch contract-net
//TODO: DroneExperiment scenario's uitdenken
//TODO: Exerpiment: betere "rapporten"
//TODO: Experiment: export to csv for raw results
class Drone(position: Point, val rng: RandomGenerator) :
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
    var batteryLevel: Double = 1.0
      private set
    var totalProfit: Double = 0.0
      private set
    var totalEstimatedProfit: Double = 0.0
        private set
    var totalEstimatedCrashes: Double = 0.0
    var totalDistanceTravelled: Double = 0.0
      private set
    var nbOrdersDelivered: Int = 0
      private set

    var crashed: Boolean = false
      private set

    //TODO experiment on influence of this
    val chargesInWarehouse: Boolean = true

    override fun afterTick(timeLapse: TimeLapse?) {
        // we don't need this in this example. This method is called after
        // all TickListener#tick() calls, hence the name.
    }

    override fun tickImpl(time: TimeLapse) {
        if (!time.hasTimeLeft() || crashed) {
            return
        }

        val messages = device?.unreadMessages

        handleMessages(time, messages?.toMutableList()?.toList()!!)

        doAction(time)
    }

    fun doAction(time: TimeLapse) {
        try{
            when(state){
                DroneState.IDLE -> moveToClosestWarehouse(time)
                DroneState.CHARGING -> charge(time)
                DroneState.CHARGING_FOR_CONTRACT -> chargeUntilMove(time)
                DroneState.PICKING_UP -> moveToPackageWarehouse(time, currentBid!!.warehouse)
                DroneState.DELIVERING -> moveToClient(time)
            }
        } catch(e: DroneCrashException) {
            //do nothing
        }
    }

    private fun moveTo(endPosition: RoadUser, time: TimeLapse) {
        val moveProgress = roadModel.moveTo(this, endPosition, time)
        totalDistanceTravelled += moveProgress.distance().value

        val startBatteryLevel = batteryLevel
        batteryLevel -= moveProgress.distance().value / DISTANCE_PER_PERCENTAGE_BATTERY_DRAIN

        if(batteryLevel < 0) {
            throw IllegalStateException("Battery empty")
        }

        val probabilityToCrash = calculateProbabilityToCrash(startBatteryLevel, batteryLevel, moveProgress.distance().value)
        if(rng.nextDouble() <= probabilityToCrash){
            crash()
        }
    }

    private fun calculateProbabilityToCrash(startBatteryLevel: Double, endBatteryLevel: Double, distance: Double): Double {
        if(!BatteryState.isSameState(startBatteryLevel, endBatteryLevel)) {
            val stateChangeLevel = BatteryState.values().map { it.upperBound }.filter { it < startBatteryLevel }.max()!!

            val distance1 = (startBatteryLevel-stateChangeLevel)/(startBatteryLevel-endBatteryLevel) * distance

            val failureFirst = calculateProbabilityToCrash(startBatteryLevel, stateChangeLevel+1e-15, distance1)
            val failureSecond = calculateProbabilityToCrash(stateChangeLevel, endBatteryLevel, distance - distance1)

            return failureFirst + (1-failureFirst) * failureSecond
        } else {
            val lambda = BatteryState.stateFromLevel(startBatteryLevel).failureLambda
            return 1 - Math.exp(- lambda * distance)
        }

    }

    fun crash() {
        //TODO: client: wat doen bij crash? => pinging to Drone + renew contract?
        totalProfit -= PRICE_DRONE

        crashed = true

        throw DroneCrashException()
    }

    fun moveToClosestWarehouse(time: TimeLapse) {
        val closest = getClosestWarehouse(realPosition)

        moveTo(closest, time)

        if(realPosition.equals(closest.position)){
            state = DroneState.CHARGING
            doAction(time)
        }
    }
    private fun getClosestWarehouse(p1: Point): Warehouse {
        return roadModel.getObjectsOfType(Warehouse::class.java).minBy { warehouse ->
            Point.distance(p1, warehouse.position)
        }!!
    }
    fun moveToPackageWarehouse(time: TimeLapse, warehouse: Warehouse) {
        moveTo(warehouse, time)

        if(realPosition.equals(warehouse.position)){
            totalProfit -= warehouse.getPriceFor(currentBid!!.order.type)
            state = DroneState.CHARGING_FOR_CONTRACT
            doAction(time)
        }
    }
    fun charge(time: TimeLapse, maxChargeDuration: Long = time.timeLeft) {
        val oldBatteryLevel = batteryLevel

        val chargeDuration = Math.min(maxChargeDuration, Math.round(Math.ceil((1.0-batteryLevel) / BATTERY_CHARGING_RATE)))
        batteryLevel += chargeDuration * BATTERY_CHARGING_RATE
        time.consume(chargeDuration)
        totalProfit -= (batteryLevel-oldBatteryLevel) * COST_FOR_ENERGY_PER_DISTANCE_UNIT * DISTANCE_PER_PERCENTAGE_BATTERY_DRAIN

        if(batteryLevel >= 1.0){
            batteryLevel = 1.0
        }
    }
    fun moveToClient(time: TimeLapse) {
        val client = currentBid!!.order.client

        moveTo(client, time)

        if(realPosition.equals(client.position)){
            state = DroneState.IDLE
            deliver(time.startTime + time.timeConsumed)
            doAction(time)
        }
    }
    fun deliver(time: Long) {
        totalProfit += currentBid!!.order.client.deliverOrder(time, currentBid!!.order)
        currentBid = null
        nbOrdersDelivered++
    }
    fun chargeUntilMove(time: TimeLapse) {
        if(!chargesInWarehouse){
            state = DroneState.DELIVERING
            doAction(time)
            return;
        }

        val distanceToClient = Point.distance(currentBid!!.warehouse.position, currentBid!!.order.client.position)
        val lastMomentToLeaveBecauseItsTime= Math.round(Math.floor(currentBid!!.order.endTime - distanceToClient / DRONE_SPEED_PER_MILLISECOND))

        val positionClient = currentBid!!.order.client.position
        val batteryLevelAtWarehouseAfterClientWithoutCharging = batteryLevel -
                batteryDrainTrajectory(currentBid!!.warehouse.position, positionClient) -
                batteryDrainTrajectory(positionClient, getClosestWarehouse(positionClient).position)
        // canLeaveBecauseSufficientlyCharged: zal met >= 20% battery bij dichtste warehouse vanaf klant aankomen
        val highestLevel = BatteryState.values().maxBy { state -> state.lowerBound }!!.lowerBound
        val canLeaveBecauseSufficientlyCharged = Math.round(Math.ceil(time.startTime + time.timeConsumed + (highestLevel - batteryLevelAtWarehouseAfterClientWithoutCharging) / BATTERY_CHARGING_RATE))


        val leaveTime: Long = Math.min(lastMomentToLeaveBecauseItsTime, canLeaveBecauseSufficientlyCharged)

        if(leaveTime <= time.startTime + time.timeConsumed || batteryLevel == 1.0) {
            state = DroneState.DELIVERING
            doAction(time)
        } else if(leaveTime > time.endTime) {
            charge(time)
        } else {
            charge(time, leaveTime - time.startTime - time.timeConsumed)
            state = DroneState.DELIVERING
            doAction(time)
        }
    }

    fun handleMessages(time: TimeLapse, messages: List<Message>) {
        // DeclareOrder
        if(state.canBid()){
            messages.filter { message -> message.contents is DeclareOrder }.forEach { message ->
                val order: Order = (message.contents as DeclareOrder).order

                val warehouse: Warehouse? = getCheapestWarehouse(order, time)
                if(warehouse != null) {
                    val costPair = estimatedCostWarehouse(warehouse, order, time.startTime)
                    val cost = -order.price + costPair.first
                    if(cost < order.fine) // Otherwise beter om order te laten vervallen
                        device?.send(BidOnOrder(Bid(order, cost, warehouse, costPair.second)), message.sender)
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

            totalEstimatedProfit += -currentBid!!.bidValue
            totalEstimatedCrashes += currentBid!!.estimatedProbabilityFailure

            // cancel other orders
            acceptOrderMessages.filter { message -> message != winningOrder }.forEach { message ->
                device?.send(CancelOrder(message.contents as AcceptOrder), message.sender)
            }
        }
    }

    private fun getCheapestWarehouse(order: Order, time: TimeLapse): Warehouse? {
        return roadModel.getObjectsOfType(Warehouse::class.java).filter { warehouse ->
            val distance = Point.distance(realPosition, warehouse.position) + Point.distance(warehouse.position, order.client.position)
            val traveltime = distance / DRONE_SPEED_PER_MILLISECOND
            val canGetInTime = time.startTime + traveltime < order.endTime

            val batteryBeforeWarehouse = batteryLevel -
                    batteryDrainTrajectory(realPosition, warehouse.position)
            var batteryLevelAtWarehouse = batteryBeforeWarehouse +
                    extraChargeInWarehouse(warehouse, order, time.startTime)
            batteryLevelAtWarehouse = Math.min(1.0, batteryLevelAtWarehouse)

            var endBatteryLevel = batteryLevelAtWarehouse -
                                  batteryDrainTrajectory(warehouse.position, order.client.position) -
                                  batteryDrainTrajectory(order.client.position, getClosestWarehouse(order.client.position).position)

            canGetInTime && endBatteryLevel > 0 && batteryBeforeWarehouse > 0
        }.minBy { warehouse ->
            estimatedCostWarehouse(warehouse, order, time.startTime).first
        }
    }

    /**
     * Import to note that this method can return > 100 %, so keep in mind that result needs to be bound to max 1.0
     */
    private fun extraChargeInWarehouse(warehouse: Warehouse, order: Order, currentTime: Long): Double {
        if(!chargesInWarehouse)
            return 0.0;

        val distance = Point.distance(realPosition, warehouse.position) + Point.distance(warehouse.position, order.client.position)
        val timeLeft = order.endTime - currentTime - distance / DRONE_SPEED_PER_MILLISECOND

        return timeLeft * BATTERY_CHARGING_RATE
    }

    private fun estimatedCostWarehouse(warehouse: Warehouse, order: Order, currentTime: Long): Pair<Double, Double> {
        val type = order.type
        val client = order.client

        val probabilityToCrashBeforeWarehouse = calculateProbabilityToCrash(batteryLevel, realPosition, warehouse.position)
        val fixedCost = warehouse.getPriceFor(type) +
                        costForEnergyOnTrajectory(realPosition, warehouse.position) +
                        costForEnergyOnTrajectory(warehouse.position, client.position) +
                        probabilityToCrashBeforeWarehouse * PRICE_DRONE

        val batteryChargingInWarehouse = extraChargeInWarehouse(warehouse, order, currentTime)
        val batteryDrainToWarehouse = batteryDrainTrajectory(realPosition, warehouse.position)
        var newBatteryLevel = batteryLevel - batteryDrainToWarehouse + batteryChargingInWarehouse
        newBatteryLevel = Math.min(newBatteryLevel, 1.0)

        val probabilityToCrashAfterWarehouse = calculateProbabilityToCrash(newBatteryLevel,  warehouse.position, client.position)
        val probabilityToCrashAfterClient = calculateProbabilityToCrash(newBatteryLevel - batteryDrainTrajectory(warehouse.position, client.position),
                client.position, getClosestWarehouse(client.position).position)

        return Pair(
                fixedCost + (1-probabilityToCrashBeforeWarehouse) * (probabilityToCrashAfterWarehouse * (PRICE_DRONE+type.marketPrice) + (1-probabilityToCrashAfterWarehouse) * probabilityToCrashAfterClient * PRICE_DRONE ),
                probabilityToCrashBeforeWarehouse + (1-probabilityToCrashBeforeWarehouse) * (probabilityToCrashAfterWarehouse + (1-probabilityToCrashAfterWarehouse) * probabilityToCrashAfterClient)
        )
    }

    private fun costForEnergyOnTrajectory(p1: Point, p2: Point)
        = Point.distance(p1, p2) * COST_FOR_ENERGY_PER_DISTANCE_UNIT

    private fun calculateProbabilityToCrash(batteryLevel: Double, p1: Point, p2: Point): Double {
        val endBatteryLevel = batteryLevel - batteryDrainTrajectory(p1, p2)
        return calculateProbabilityToCrash(batteryLevel, endBatteryLevel, Point.distance(p1, p2))
    }

    private fun batteryDrainTrajectory(p1: Point, p2: Point): Double
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

enum class BatteryState(val lowerBound: Double, val upperBound: Double, val failureLambda: Double) {
    CRITICAL(Double.NEGATIVE_INFINITY, 0.1, LAMBDA_CRITICAL),
    LOW(0.1, 0.2, LAMBDA_LOW),
    NORMAL(0.2, Double.POSITIVE_INFINITY, LAMBDA_NORMAL);

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
