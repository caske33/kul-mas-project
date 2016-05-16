package example.experiment


import com.github.rinde.rinsim.core.model.comm.CommModel
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders
import com.github.rinde.rinsim.experiment.Experiment
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult
import com.github.rinde.rinsim.experiment.ExperimentResults
import com.github.rinde.rinsim.experiment.MASConfiguration
import com.github.rinde.rinsim.scenario.Scenario
import com.github.rinde.rinsim.scenario.StopConditions
import com.github.rinde.rinsim.ui.View
import com.github.rinde.rinsim.ui.renderers.DroneCommRenderer
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer
import com.google.common.base.Optional
import example.Client
import example.Drone
import example.*
import example.Warehouse

object DroneExperiment {
    val MAX_TIME_SCENARIO = 3 * 60 * 60 * 1000

    val NUMBER_WAREHOUSES = 3
    val NUMBER_DRONES = 3
    val NUMBER_INITIAL_CLIENTS = 3
    val NUMBER_EXTRA_CLIENTS = 3

    @JvmStatic fun main(args: Array<String>) {
        val uiSpeedUp = 4

        val results: Optional<ExperimentResults>

        results = Experiment.builder()
                .addConfiguration(MASConfiguration.builder()
                        .addEventHandler(AddClientEvent::class.java, AddClientEventHandler())
                        .addEventHandler(AddDroneEvent::class.java, AddDroneEventHandler())
                        .addEventHandler(AddWarehouseEvent::class.java, AddWarehouseEventHandler())
                        .addModel(CommModel.builder()).build())
                .addScenario(createScenario(MAX_TIME_SCENARIO.toLong()))
                .repeat(2)
                .withRandomSeed(RANDOM_SEED)
                .withThreads(1)
                .usePostProcessor(ExperimentPostProcessor())
                .showGui(View.builder()
                        .with(PlaneRoadModelRenderer.builder())
                        .withResolution(800, 800)
                        .withAutoPlay()
                        .withAutoClose()
                        .withSpeedUp(uiSpeedUp)
                        .with(RoadUserRenderer.builder()
                                .withImageAssociation(Drone::class.java, "/graphics/perspective/semi-truck-32.png")
                                .withImageAssociation(Client::class.java, "/graphics/flat/deliverylocation.png")
                                .withImageAssociation(Warehouse::class.java, "/graphics/flat/warehouse-32.png"))
                        .with(DroneCommRenderer.builder()
                                .withBatteryLevel()
                                .withProfit())
                        .withTitleAppendix("Experiments DroneWorld"))
                .perform(System.out, *args)

        if (results.isPresent) {
            for (sr in results.get().results) {
                // The SimulationResult contains all information about a specific
                // simulation, the result object is the object created by the post
                // processor, a String in this case.
                println("${sr.simArgs.randomSeed} ${sr.resultObject}")
            }
        } else {
            throw IllegalStateException("DroneExperiment did not complete.")
        }
    }

    /**
     * Defines a simple scenario with one depot, one vehicle and three parcels.
     * Note that a scenario is supposed to only contain problem specific
     * information it should (generally) not make any assumptions about the
     * algorithm(s) that are used to solve the problem.
     * @return A newly constructed scenario.
     */
    internal fun createScenario(scenarioLength: Long): Scenario {
        var builder: Scenario.Builder = Scenario.builder()
                .scenarioLength(scenarioLength)
                .addModel(RoadModelBuilders.plane()
                        .withMinPoint(MIN_POINT)
                        .withMaxPoint(MAX_POINT)
                        .withMaxSpeed(DRONE_SPEED))
                .addModel(DefaultPDPModel.builder())
                //TODO: stop when all drones fully charged at warehouse?
                .setStopCondition(StopConditions.limitedTime(scenarioLength))


        for (i in 0..NUMBER_WAREHOUSES - 1) {
            builder = builder.addEvent(AddWarehouseEvent())
        }
        for (i in 0..NUMBER_DRONES - 1) {
            builder = builder.addEvent(AddDroneEvent())
        }
        for (i in 0..NUMBER_INITIAL_CLIENTS - 1) {
            builder = builder.addEvent(AddClientEvent(0))
        }
        for (i in 0..NUMBER_EXTRA_CLIENTS - 1) {
            val time = (i + 1) * (scenarioLength / 2) / NUMBER_EXTRA_CLIENTS
            builder = builder.addEvent(AddClientEvent(time))
        }

        return builder.build()
    }
}
