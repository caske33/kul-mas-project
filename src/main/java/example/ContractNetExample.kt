package example

import org.apache.commons.math3.random.RandomGenerator

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.comm.CommDevice
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommModel
import com.github.rinde.rinsim.core.model.comm.Message
import com.github.rinde.rinsim.core.model.comm.MessageContents
import com.github.rinde.rinsim.core.model.road.MovingRoadUser
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.github.rinde.rinsim.ui.View
import com.github.rinde.rinsim.ui.renderers.CommRenderer
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.pdp.*
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList


object ContractNetExample {

    internal val VEHICLE_SPEED_KMH = 50.0
    internal val MIN_POINT = Point(0.0, 0.0)
    internal val MAX_POINT = Point(10.0, 10.0)
    internal val TICK_LENGTH = 1000L
    internal val RANDOM_SEED = 123L
    internal val NUM_DRONES = 3

    internal val TEST_SPEEDUP = 1
    internal val TEST_STOP_TIME = 10 * 60 * 1000.toLong()
    private val NUM_PACKAGES = 1
    private val SERVICE_DURATION: Long = 60000
    private val MAX_CAPACITY = 5
    private val NEW_CUSTOMER_PROB = .03141567841510015464654654654

    private val NUM_HUBS = 4
    private val NUM_CLIENTS = 4


    /**
     * Starts the example.
     * @param args This is ignored.
     */
    @JvmStatic fun main(args: Array<String>) {
        run(false)
    }

    /**
     * Run the example.
     * @param testing if `true` turns on testing mode.
     */
    fun run(testing: Boolean) {
        // configure the GUI. We use separate renderers for the road model and
        // for the drivers. By default the road model is rendered as a square
        // (indicating its boundaries), and the drivers are rendered as red
        // dots.
        var viewBuilder: View.Builder = View.builder().with(PlaneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder()
                    .withImageAssociation(Drone::class.java, "/graphics/perspective/semi-truck-32.png")
                    .withImageAssociation(Order::class.java, "/graphics/perspective/deliverypackage.png")
                    .withImageAssociation(Client::class.java, "/graphics/flat/deliverylocation.png")
                    .withImageAssociation(Hub::class.java, "/graphics/flat/warehouse-32.png"))
                .with(CommRenderer.builder().withReliabilityColors().withMessageCount())

        if (testing) {
            viewBuilder = viewBuilder.withSpeedUp(TEST_SPEEDUP).withAutoClose().withAutoPlay().withSimulatorEndTime(TEST_STOP_TIME)
        }

        // initialize a new Simulator instance
        val sim = Simulator.builder().setTickLength(TICK_LENGTH).setRandomSeed(RANDOM_SEED).addModel(
                RoadModelBuilders.plane().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(VEHICLE_SPEED_KMH)).addModel(viewBuilder).addModel(DefaultPDPModel.builder()).addModel(CommModel.builder()).build()

        val roadModel = sim.getModelProvider().getModel(RoadModel::class.java)
        val rng = sim.getRandomGenerator()
        val device = null

        // add a number of drones on the road
        for (i in 0..NUM_DRONES - 1) {
            sim.register(Drone(roadModel.getRandomPosition(rng)))
        }

        for (i in 0..NUM_HUBS - 1) {
            sim.register(Hub(roadModel.getRandomPosition(rng)))
        }

        for (i in 0..NUM_CLIENTS - 1) {
            sim.register(Client(roadModel.getRandomPosition(rng)))
        }

        val hubIterator = roadModel.getObjectsOfType(Hub::class.java).iterator()
        // add a number of packages
        for (i in 0..NUM_HUBS - 1) { // TODO for now 1 package per hub
            val hub : Hub = hubIterator.next()
            sim.register(Order(Parcel.builder(hub.position.get(),
                    roadModel.getObjectsOfType(Client::class.java).toList().get(i).position!!.get()).serviceDuration(SERVICE_DURATION).neededCapacity(1.0) // + rng.nextInt(MAX_CAPACITY))
                    .buildDTO(), PackageType.IPOD, hub))
        }



        sim.addTickListener(object : TickListener {
            override fun tick(time: TimeLapse) {
                if (time.startTime > Integer.MAX_VALUE) {
                    sim.stop()
                } /*else if (rng.nextDouble() < NEW_CUSTOMER_PROB) {
                    sim.register(Package(
                            Parcel.builder(roadModel.getRandomPosition(rng),
                                    roadModel.getRandomPosition(rng)).serviceDuration(SERVICE_DURATION).neededCapacity((1 + rng.nextInt(MAX_CAPACITY)).toDouble()).buildDTO()))
                }*/
            }

            override fun afterTick(timeLapse: TimeLapse) {
                // TODO Auto-generated method stub

            }
        })

        // if a GUI is added, it starts it, if no GUI is specified it will
        // run the simulation without visualization.
        sim.start()
    }

    internal class Drone(var position: Point) : Vehicle(VehicleDTO.builder().capacity(1).startPosition(position).speed(ContractNetExample.Drone.SPEED).build()), MovingRoadUser, TickListener, CommUser {
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
                    if (message.contents is WinningBidMessage) {
                        if (dynamic && hasContract) {
                            device?.send(CancelMessage(), currentOrder!!.origin)
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
                        device?.send(BiddingMessage(currentDistance, order), hub)
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

    internal class Order(dto : ParcelDTO, var content : PackageType, var origin : Hub) : Parcel(dto), TickListener {
        // origin = Hub
        // pickupLocation = Hub position
        // deliveryLocation = Client

        var isPickedUp = false
        var isDelivered = false

        override fun tick(timeLapse: TimeLapse?) {
            //
        }

        override fun afterTick(timeLapse: TimeLapse?) {
            //
        }

        fun getPosition(): Optional<Point> {
            val rm = roadModel
            if (rm.containsObject(this)) {
                return Optional.of(rm.getPosition(this))
            }
            return Optional.absent<Point>()

        }
    }

    internal class Central(location: Point) : Depot(location), TickListener, CommUser {
        override fun afterTick(timeLapse: TimeLapse?) {
            throw UnsupportedOperationException()
        }

        override fun getPosition(): Optional<Point>? {
            throw UnsupportedOperationException()
        }

        override fun setCommDevice(builder: CommDeviceBuilder?) {
            throw UnsupportedOperationException()
        }

        override fun tick(timeLapse: TimeLapse?) {
            throw UnsupportedOperationException()
        }
    }

    internal class Plane(var position: Point) : Vehicle(VehicleDTO.builder().capacity(350).startPosition(position).speed(ContractNetExample.Plane.SPEED).build()), MovingRoadUser, TickListener, CommUser {
        override fun setCommDevice(builder: CommDeviceBuilder?) {
            throw UnsupportedOperationException()
        }

        override fun getPosition(): Optional<Point>? {
            throw UnsupportedOperationException()
        }

        override fun tickImpl(time: TimeLapse?) {
            throw UnsupportedOperationException()
        }

        companion object {
            private val SPEED = 500.0
        }
    }

    internal class Hub(location: Point) : Depot(location), TickListener, CommUser {


        var device: CommDevice? = null

        var hasClientContract = false
        var pickedUp = false
        var messageBroadcast = false

        var order: Order? = null
            get() = getOrders()

        fun getOrders(): Order? {
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

        var currentOrder : PackageType? = null

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
            println(pdpModel.getContents(this))

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

    internal class Client(location: Point) : Depot(location), TickListener, CommUser {

        private var hasContract : Boolean = false
        private var messageBroadcast : Boolean = false
        private var device : CommDevice? = null

        override fun afterTick(timeLapse: TimeLapse?) {
            //
        }

        override fun tick(timeLapse: TimeLapse?) {
                if (!hasContract && !messageBroadcast) {
                    device?.broadcast(ClientOfferMessage(PackageType.IPOD))
                    messageBroadcast = true
                }
                if (!hasContract && messageBroadcast) {
                    var bestBid = Integer.MAX_VALUE.toInt().toDouble()
                    var bestVehicle: Hub? = null
                    val messages = device?.unreadMessages ?: ImmutableList.of()
                    for (i in messages.indices) {
                        val message = messages[i]
                        if (message.contents is BiddingMessage && message.sender is Hub) {
                            val contents = message.contents as BiddingMessage

                            val bid = contents.bid
                            if (bid < bestBid) {
                                bestBid = bid
                                bestVehicle = message.sender as Hub
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
    }

    internal class BiddingMessage(var bid: Double, var order: Order?) : MessageContents

    internal class CancelMessage() : MessageContents

    internal class WinningBidMessage(var order: Order?) : MessageContents

    internal class ClientOfferMessage(var order: PackageType?) : MessageContents

    internal class WinningClientBidMessage(var order : PackageType?) : MessageContents

    internal class HubOfferMessage(var order: Order?) : MessageContents

    internal enum class Messages : MessageContents {
        PICK_ME_UP, I_CHOOSE_YOU
    }

    internal enum class PackageType {
        CAMERA, COOKIE, IPOD
    }
}