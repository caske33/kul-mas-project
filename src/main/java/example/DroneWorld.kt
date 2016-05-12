package example


import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.comm.CommModel
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders
import com.github.rinde.rinsim.geom.Point
import com.github.rinde.rinsim.ui.View
import com.github.rinde.rinsim.ui.renderers.CommRenderer
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer
import com.github.rinde.rinsim.core.model.pdp.*
import com.github.rinde.rinsim.ui.renderers.DroneCommRenderer

object DroneWorld {

    //TODO move to Variables
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
    private val NUM_INITIAL_CLIENTS = 4


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
                    .withImageAssociation(Warehouse::class.java, "/graphics/flat/warehouse-32.png"))
                .with(DroneCommRenderer.builder()
                        .withMessageCount()
                        .withBatteryLevel()
                        .withProfit())

        if (testing) {
            viewBuilder = viewBuilder
                    .withSpeedUp(TEST_SPEEDUP)
                    .withAutoClose()
                    .withAutoPlay()
                    .withSimulatorEndTime(TEST_STOP_TIME)
        }

        // initialize a new Simulator instance
        val sim = Simulator.builder()
                .setTickLength(TICK_LENGTH)
                .setRandomSeed(RANDOM_SEED)
                .addModel(
                    RoadModelBuilders.plane()
                            .withMinPoint(MIN_POINT)
                            .withMaxPoint(MAX_POINT)
                            .withMaxSpeed(DRONE_SPEED)
                ).addModel(viewBuilder)
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .build()

        val roadModel = sim.getModelProvider().getModel(RoadModel::class.java)
        val rng = sim.getRandomGenerator()

        // add a number of drones on the road
        for (i in 0..NUM_DRONES - 1) {
            sim.register(Drone(roadModel.getRandomPosition(rng), rng))
        }

        for (i in 0..NUM_HUBS - 1) {
            sim.register(Warehouse(roadModel.getRandomPosition(rng), rng))
        }

        for (i in 0..NUM_INITIAL_CLIENTS - 1) {
            sim.register(Client(roadModel.getRandomPosition(rng), rng, sim))
        }

        /*sim.addTickListener(object : TickListener {
            override fun tick(time: TimeLapse) {
                if (time.startTime > Integer.MAX_VALUE) {
                    sim.stop()
                } else if (rng.nextDouble() < NEW_CUSTOMER_PROB) {
                    sim.register(Client(roadModel.getRandomPosition(rng), rng, sim))
                }
            }

            override fun afterTick(timeLapse: TimeLapse) {
            }
        })*/

        // if a GUI is added, it starts it, if no GUI is specified it will
        // run the simulation without visualization.
        sim.start()
    }
}