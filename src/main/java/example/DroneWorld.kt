package example


import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.comm.CommModel
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders
import com.github.rinde.rinsim.geom.Point
import com.github.rinde.rinsim.ui.View
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer
import com.github.rinde.rinsim.core.model.pdp.*
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.ui.renderers.DroneCommRenderer
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer
import javax.measure.unit.SI

object DroneWorld {
    val NUM_DRONES = 5

    val TEST_SPEEDUP = 1
    val TEST_STOP_TIME = 10 * 60 * 1000.toLong()
    val NEW_CUSTOMER_PROB = .03141567841510015464654654654

    val NUM_HUBS = 8
    val NUM_INITIAL_CLIENTS = 50


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
                    .withImageAssociation(Client::class.java, "/graphics/flat/deliverylocation.png")
                    .withImageAssociation(Warehouse::class.java, "/graphics/flat/warehouse-32.png"))
                .with(DroneCommRenderer.builder()
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
                            //.withDistanceUnit(SI.METER)
                            //.withSpeedUnit(SI.METERS_PER_SECOND)
                ).addModel(viewBuilder)
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .build()

        val roadModel = sim.getModelProvider().getModel(RoadModel::class.java)
        val rng = sim.getRandomGenerator()

        // add a number of drones on the road
        for (i in 0..NUM_DRONES - 1) {
            sim.register(Drone(roadModel.getRandomPosition(rng), rng, true, ProtocolType.CONTRACT_NET_CONFIRMATION))
        }

        for (i in 0..NUM_HUBS - 1) {
            sim.register(Warehouse(roadModel.getRandomPosition(rng), rng))
        }

        for (i in 0..NUM_INITIAL_CLIENTS - 1) {
            sim.register(Client(roadModel.getRandomPosition(rng), rng, sim, ProtocolType.CONTRACT_NET_CONFIRMATION))
        }

        sim.addTickListener(object : TickListener {
            override fun tick(time: TimeLapse) {
                if (time.startTime > Integer.MAX_VALUE) {
                    sim.stop()
                } else if (rng.nextDouble() < NEW_CUSTOMER_PROB) {
                    sim.register(Client(roadModel.getRandomPosition(rng), rng, sim, ProtocolType.CONTRACT_NET_CONFIRMATION))
                }
            }

            override fun afterTick(timeLapse: TimeLapse) {
            }
        })

        // if a GUI is added, it starts it, if no GUI is specified it will
        // run the simulation without visualization.
        sim.start()
    }
}