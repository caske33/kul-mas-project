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

class Drone(position: Point, val rng: RandomGenerator, val chargesInWarehouse: Boolean, val protocolType: ProtocolType) :
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

    var currentBid: Bid? = null
      private set
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

    val warehouses: Set<Warehouse> by lazy {
        roadModel!!.getObjectsOfType(Warehouse::class.java)
    }

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
                DroneState.PROPOSED_BID -> moveTo(currentBid!!.warehouse, time)
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
        batteryLevel -= moveProgress.distance().value / DISTANCE_PER_FULL_BATTERY

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
        totalProfit -= PRICE_DRONE

        crashed = true

        if(currentBid != null)
            device?.send(Failure(), currentBid!!.order.client)

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
        return warehouses.minBy { warehouse ->
            Point.distance(p1, warehouse.position)
        }!!
    }
    fun moveToPackageWarehouse(time: TimeLapse, warehouse: Warehouse) {
        moveTo(warehouse, time)

        if(realPosition.equals(warehouse.position)){
            state = DroneState.CHARGING_FOR_CONTRACT
            doAction(time)
        }
    }
    fun charge(time: TimeLapse, maxChargeDuration: Long = time.timeLeft) {
        val oldBatteryLevel = batteryLevel

        val chargeDuration = Math.min(maxChargeDuration, Math.round(Math.ceil((1.0-batteryLevel) / BATTERY_CHARGING_RATE)))
        batteryLevel += chargeDuration * BATTERY_CHARGING_RATE
        time.consume(chargeDuration)
        totalProfit -= (batteryLevel-oldBatteryLevel) * COST_FOR_ENERGY_PER_DISTANCE_UNIT * DISTANCE_PER_FULL_BATTERY

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
        totalProfit += calculateProfit(time, currentBid!!.order)
        device?.send(InformDone(currentBid!!.order, time), currentBid!!.order.client)
        currentBid = null
        nbOrdersDelivered++
    }
    private fun calculateProfit(deliveryTime: Long, order: Order): Double {
        if(deliveryTime < order.endTime)
            return order.price
        else
            return -order.fine
    }

    fun chargeUntilMove(time: TimeLapse) {
        if(!chargesInWarehouse){
            startDelivering(time)
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
            startDelivering(time)
        } else if(leaveTime > time.endTime) {
            charge(time)
        } else {
            charge(time, leaveTime - time.startTime - time.timeConsumed)
            startDelivering(time)
        }
    }

    fun startDelivering(time: TimeLapse) {
        totalProfit -= currentBid!!.warehouse.getPriceFor(currentBid!!.order.type)
        device?.send(InformPickedUp(currentBid!!.order, time.startTime + time.timeConsumed), currentBid!!.order.client)

        state = DroneState.DELIVERING
        doAction(time)
    }

    fun handleMessages(time: TimeLapse, messages: List<Message>) {
        // GotBetterOffer
        messages.filter { message -> message.contents is GotBetterOffer }.forEach { message ->
            if(currentBid != null && (message.contents as GotBetterOffer).orderThatIsLost == currentBid!!.order) {
                currentBid = null;
                state = DroneState.IDLE
            }
        }

        // RejectProposal
        val rejectProposalMessages = messages.filter { message -> message.contents is RejectProposal }
        rejectProposalMessages.forEach { message ->
            val contents = message.contents as RejectProposal
            if(contents.bid == currentBid){
                state = DroneState.IDLE
                currentBid = null
            }
        }

        // AcceptProposal
        //TODO polymorphism?
        val acceptProposalMessages = messages.filter { message -> message.contents is AcceptProposal }
        if(protocolType == ProtocolType.CONTRACT_NET){
            acceptProposalMessages.forEach { message ->
                val contents = message.contents as AcceptProposal
                if(contents.bid == currentBid){
                    state = DroneState.PICKING_UP
                    totalEstimatedProfit += -currentBid!!.bidValue
                    totalEstimatedCrashes += currentBid!!.estimatedProbabilityFailure
                }
            }
        } else if(canNegotiate() && acceptProposalMessages.size > 0){
            // accept order with lowest (estimated) cost
            val winningOrderMessage: Message = acceptProposalMessages.minBy { message -> (message.contents as AcceptProposal).bid.bidValue }!!
            val winningBid = (winningOrderMessage.contents as AcceptProposal).bid

            if(currentBid != null && currentBid!!.order != winningBid.order) {
                device?.send(Disagree(currentBid!!), currentBid!!.order.client)
            }

            //TODO don't send agree in dynamisch contract net
            device?.send(Agree(winningBid), winningOrderMessage.sender)
            currentBid = winningBid
            state = DroneState.PICKING_UP

            totalEstimatedProfit += -currentBid!!.bidValue
            totalEstimatedCrashes += currentBid!!.estimatedProbabilityFailure

            // cancel other orders
            acceptProposalMessages.filter { message -> message != winningOrderMessage }.forEach { message ->
                device?.send(Disagree((message.contents as AcceptProposal).bid), message.sender)
            }
        } else {
            acceptProposalMessages.forEach { message ->
                device?.send(Disagree((message.contents as AcceptProposal).bid), message.sender)
            }
        }

        // CallForProposal
        val callForProposals = messages.filter { message -> message.contents is CallForProposal }
        if(!canNegotiate()){
            callForProposals.forEach { message ->
                device?.send(Refuse((message.contents as CallForProposal).order, RefuseReason.BUSY), message.sender)
            }
        } else {
            val bids = callForProposals.map { message ->
                val order: Order = (message.contents as CallForProposal).order

                val warehouse: Pair<Warehouse, Pair<Double, Double>>? = getCheapestWarehouse(order, time)
                var bid: Bid? = null
                if(warehouse != null) {
                    val costPair = warehouse.second
                    val cost = -order.price + costPair.first
                    bid = Bid(order, cost, warehouse.first, costPair.second)
                }

                Triple(message, order, bid)
            }

            bids.filter{ triple -> triple.third == null }.forEach { triple ->
                device?.send(Refuse(triple.second, RefuseReason.INELIGIBLE), triple.first.sender)
            }

            val realBids = bids.filter { triple -> triple.third != null }

            if(protocolType == ProtocolType.CONTRACT_NET) {
                val min = realBids.minBy { triple ->
                    triple.third!!.bidValue
                }
                if(min != null) {
                    state = DroneState.PROPOSED_BID
                    currentBid = min.third
                    device?.send(Propose(min.third!!), min.first.sender)
                    realBids.forEach { triple ->
                        if(triple != min){
                            device?.send(Refuse(triple.second, RefuseReason.LOW_RANKING), triple.first.sender)
                        }
                    }
                }
            } else {
                realBids.forEach { triple ->
                    device?.send(Propose(triple.third!!), triple.first.sender)
                }
            }
        }
    }

    private fun getCheapestWarehouse(order: Order, time: TimeLapse): Pair<Warehouse, Pair<Double, Double>>? {
        return warehouses.filter { warehouse ->
            val distance = Point.distance(realPosition, warehouse.position) + Point.distance(warehouse.position, order.client.position)
            val traveltime = distance / DRONE_SPEED_PER_MILLISECOND
            val canGetInTime = time.startTime + traveltime < order.endTime

            val batteryBeforeWarehouse = batteryLevel -
                    batteryDrainTrajectory(realPosition, warehouse.position)
            var batteryLevelAtWarehouse = batteryBeforeWarehouse +
                    extraChargeInWarehouse(warehouse, order, time.startTime)
            batteryLevelAtWarehouse = Math.min(1.0, batteryLevelAtWarehouse)

            val endBatteryLevel = batteryLevelAtWarehouse -
                                  batteryDrainTrajectory(warehouse.position, order.client.position) -
                                  batteryDrainTrajectory(order.client.position, getClosestWarehouse(order.client.position).position)

            canGetInTime && endBatteryLevel > 0 && batteryBeforeWarehouse > 0
        }.map { warehouse ->
            Pair(warehouse, estimatedCostWarehouse(warehouse, order, time.startTime))
        }.filter { pair ->
            pair.second.first - order.price < order.fine
        }.minBy { pair ->
            pair.second.first
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
        = Point.distance(p1, p2) / DISTANCE_PER_FULL_BATTERY

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

    fun canNegotiate() = state.canNegotiate(protocolType)
}

enum class DroneState() {
    IDLE,
    PROPOSED_BID,
    PICKING_UP,
    CHARGING,
    DELIVERING,
    CHARGING_FOR_CONTRACT;

    fun canNegotiate(protocolType: ProtocolType): Boolean =
        when(protocolType) {
            ProtocolType.CONTRACT_NET -> this == IDLE || this == CHARGING
            ProtocolType.CONTRACT_NET_CONFIRMATION -> this == IDLE || this == CHARGING
            ProtocolType.DYNAMIC_CONTRACT_NET -> this != DELIVERING
        }
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
