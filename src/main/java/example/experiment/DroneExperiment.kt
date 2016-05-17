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
    val MAX_TIME_SCENARIO: Long = 2 * 60 * 60 * 1000

    @JvmStatic fun main(args: Array<String>) {
        val uiSpeedUp = 1
        val withGui: Boolean = true

        var builder = Experiment.builder()
                .addConfiguration(MASConfiguration.builder()
                        .addEventHandler(AddClientEvent::class.java, AddClientEventHandler())
                        .addEventHandler(AddDroneEvent::class.java, AddDroneEventHandler())
                        .addEventHandler(AddWarehousesEvent::class.java, AddWarehouseEventHandler())
                        .addModel(CommModel.builder()).build())
                //.addScenarios(createScenariosWithMoreDrones(MAX_TIME_SCENARIO, 15, 1, 10, 2, 5, 5))
                .addScenario(createScenario(MAX_TIME_SCENARIO, true, false, 10, 7, 10, 10))
                .addScenario(createScenario(MAX_TIME_SCENARIO, true, true, 10, 7, 10, 10))
                .withRandomSeed(RANDOM_SEED)
                .usePostProcessor(ExperimentPostProcessor())

        if(withGui){
            builder = builder
                    .withThreads(1)
                    .repeat(1)
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

        val results_: Set<SimulationResult> = builder.perform(System.out, *args).get().results
        val results = results_.map { SimulationExperimentResult(it.simArgs, it.resultObject as ExperimentResult) }

        for (sr in results) {
            println("${sr.resultObject}")
        }
        results.groupBy { it.simArgs.randomSeed }.mapValues { averageFromResults(it.value) }.forEach {
            println("${it.key} averaged € ${it.value} profit")
        }
        results.groupBy { it.simArgs.scenario }.mapValues { averageFromResults(it.value) }.forEach {
            println("averaged € ${it.value} profit")
        }
        val aggregated = sumFromResults(results)
        println(aggregated.estimatedTotalProfit)
        println(aggregated.totalProfit)
        println(aggregated.estimatedNbCrashes)
        println(aggregated.nbCrashes)
        results.groupBy { it.simArgs.scenario }.mapValues {
            val aggregated = sumFromResults(it.value)
            listOf<Double>(
                    aggregated.estimatedTotalProfit,
                    aggregated.totalProfit,
                    aggregated.estimatedNbCrashes,
                    aggregated.nbCrashes
        ) }.forEach {
            println("per scenario: ${it.value}")
        }
                /*
        results.results.groupBy { it.simArgs.scenario }.mapValues { it.value.map { listOf<Double>(
                0.0
        ) }.forEach {
            println("averaged € ${it} profit")
        }*/
    }

    /**
     * Defines a simple scenario with one depot, one vehicle and three parcels.
     * Note that a scenario is supposed to only contain problem specific
     * information it should (generally) not make any assumptions about the
     * algorithm(s) that are used to solve the problem.
     * @return A newly constructed scenario.
     */
    internal fun createScenario(lastClientAddTime: Long, chargesInWarehouse: Boolean, withDynamicContractNet: Boolean, nbWarehouses: Int, nbDrones: Int, nbInitialClients: Int, nbExtraClients: Int): Scenario {
        var builder: Scenario.Builder = Scenario.builder()
                .scenarioLength(lastClientAddTime)
                .addModel(RoadModelBuilders.plane()
                        .withMinPoint(MIN_POINT)
                        .withMaxPoint(MAX_POINT)
                        .withMaxSpeed(DRONE_SPEED))
                .addModel(DefaultPDPModel.builder())
                .setStopCondition(StopConditions.or(
                        DronesBackAtWarehouseAndOrdersDoneStopCondition(lastClientAddTime),
                        StopConditions.limitedTime(lastClientAddTime * 10)
                ))

        builder = builder.addEvent(AddWarehousesEvent(nbWarehouses))
        for (i in 1..nbDrones) {
            builder = builder.addEvent(AddDroneEvent(chargesInWarehouse, withDynamicContractNet))
        }
        for (i in 1..nbInitialClients) {
            builder = builder.addEvent(AddClientEvent(0, withDynamicContractNet))
        }
        for (i in 1..nbExtraClients) {
            val time = i * lastClientAddTime / nbExtraClients
            builder = builder.addEvent(AddClientEvent(time, withDynamicContractNet))
        }
        return builder.build()
    }

    internal fun createScenariosWithMoreDrones(scenarioLength: Long, nbWarehouses: Int,
                                               nbDronesMin: Int, nbDronesMax: Int, nbDronesStep: Int,
                                               nbInitialClients: Int, nbExtraClients: Int): List<Scenario> {
        //TODO variate with chargesInWarehouse + withDynamicContractNet
        return (nbDronesMin..nbDronesMax step nbDronesStep).map {
            createScenario(scenarioLength, true, false, nbWarehouses, it, nbInitialClients, nbExtraClients)
        }
    }
}
