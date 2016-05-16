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
    val MAX_TIME_SCENARIO: Long = 3 * 60 * 60 * 1000

    @JvmStatic fun main(args: Array<String>) {
        val uiSpeedUp = 16
        val testing: Boolean = false

        var builder = Experiment.builder()
                .addConfiguration(MASConfiguration.builder()
                        .addEventHandler(AddClientEvent::class.java, AddClientEventHandler())
                        .addEventHandler(AddDroneEvent::class.java, AddDroneEventHandler())
                        .addEventHandler(AddWarehousesEvent::class.java, AddWarehouseEventHandler())
                        .addModel(CommModel.builder()).build())
                .addScenarios(createScenariosWithMoreDrones(MAX_TIME_SCENARIO, 15, 1, 10, 2, 5, 5))
                .withRandomSeed(RANDOM_SEED)
                .usePostProcessor(ExperimentPostProcessor())

        if(testing){
            builder = builder
                    .withThreads(1)
                    .repeat(2)
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

        } else {
            builder = builder
                    .withThreads(8)
                    .repeat(50*2)
        }

        val results = builder.perform(System.out, *args).get()

        for (sr in results.results) {

            // The SimulationResult contains all information about a specific
            // simulation, the result object is the object created by the post
            // processor, a String in this case.
            println("${sr.simArgs.randomSeed} ${sr.resultObject}")
        }
        results.results.groupBy { it.simArgs.randomSeed }.mapValues { it.value.map { (it.resultObject as ExperimentResult).totalProfit }.average() }.forEach {
            println("${it.key} averaged € ${it.value} profit")
        }
        results.results.groupBy { it.simArgs.scenario }.mapValues { it.value.map { (it.resultObject as ExperimentResult).totalProfit }.average() }.forEach {
            println("averaged € ${it.value} profit")
        }
        println(results.results.map { (it.resultObject as ExperimentResult).estimatedTotalProfit  }.sum())
        println(results.results.map { (it.resultObject as ExperimentResult).totalProfit  }.sum())
        println(results.results.map { (it.resultObject as ExperimentResult).nbCrashes  }.sum())
    }

    /**
     * Defines a simple scenario with one depot, one vehicle and three parcels.
     * Note that a scenario is supposed to only contain problem specific
     * information it should (generally) not make any assumptions about the
     * algorithm(s) that are used to solve the problem.
     * @return A newly constructed scenario.
     */
    internal fun createScenario(scenarioLength: Long, nbWarehouses: Int, nbDrones: Int, nbInitialClients: Int, nbExtraClients: Int): Scenario {
        var builder: Scenario.Builder = Scenario.builder()
                .scenarioLength(scenarioLength)
                .addModel(RoadModelBuilders.plane()
                        .withMinPoint(MIN_POINT)
                        .withMaxPoint(MAX_POINT)
                        .withMaxSpeed(DRONE_SPEED))
                .addModel(DefaultPDPModel.builder())
                //TODO: stop when all drones fully charged at warehouse?
                .setStopCondition(StopConditions.limitedTime(scenarioLength))

        builder = builder.addEvent(AddWarehousesEvent(nbWarehouses))
        for (i in 1..nbDrones) {
            builder = builder.addEvent(AddDroneEvent())
        }
        for (i in 1..nbInitialClients) {
            builder = builder.addEvent(AddClientEvent(0))
        }
        for (i in 1..nbExtraClients) {
            val time = i * (scenarioLength / 2) / nbExtraClients
            builder = builder.addEvent(AddClientEvent(time))
        }
        return builder.build()
    }

    internal fun createScenariosWithMoreDrones(scenarioLength: Long, nbWarehouses: Int,
                                               nbDronesMin: Int, nbDronesMax: Int, nbDronesStep: Int,
                                               nbInitialClients: Int, nbExtraClients: Int): List<Scenario> {
        return (nbDronesMin..nbDronesMax step nbDronesStep).map { createScenario(scenarioLength, nbWarehouses, it, nbInitialClients, nbExtraClients) }
    }
}
