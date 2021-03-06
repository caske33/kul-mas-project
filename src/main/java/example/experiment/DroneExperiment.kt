package example.experiment


import com.github.rinde.rinsim.core.model.comm.CommModel
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders
import com.github.rinde.rinsim.experiment.Experiment
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult
import com.github.rinde.rinsim.experiment.MASConfiguration
import com.github.rinde.rinsim.scenario.Scenario
import com.github.rinde.rinsim.scenario.StopConditions
import com.github.rinde.rinsim.ui.View
import com.github.rinde.rinsim.ui.renderers.DroneCommRenderer
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer
import example.Client
import example.Drone
import example.*
import example.Warehouse

object DroneExperiment {
    val MAX_TIME_SCENARIO: Long = 1 * 60 * 60 * 1000

    @JvmStatic fun main(args: Array<String>) {
        val uiSpeedUp = 1
        val withGui: Boolean = false

        var builder = Experiment.builder()
                .addConfiguration(MASConfiguration.builder()
                        .addEventHandler(AddClientsEvent::class.java, AddClientsEventHandler())
                        .addEventHandler(AddDroneEvent::class.java, AddDroneEventHandler())
                        .addEventHandler(AddWarehousesEvent::class.java, AddWarehouseEventHandler())
                        .addModel(CommModel.builder()).build())
                //.addScenario(createScenario(MAX_TIME_SCENARIO, DroneProblemClass(true, ProtocolType.CONTRACT_NET, 4, 3, 10, 10)))
                //.addScenario(createScenario(MAX_TIME_SCENARIO, DroneProblemClass(true, ProtocolType.CONTRACT_NET_CONFIRMATION, 4, 3, 10, 10)))
                //.addScenario(createScenario(MAX_TIME_SCENARIO, DroneProblemClass(true, ProtocolType.DYNAMIC_CONTRACT_NET, 4, 3, 10, 10)))
                //.addScenarios(createScenariosForExperiment(MAX_TIME_SCENARIO, Integer.parseInt(args[0])))
                .addScenarios(createScenariosForSecondExperiment(MAX_TIME_SCENARIO, Integer.parseInt(args[0])))
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
                                    .withMessageCount()
                                    .withBatteryLevel()
                                    .withProfit())
                            .withTitleAppendix("Experiments DroneWorld"))

        } else {
            builder = builder
                    .withThreads(8)
                    .repeat(150)
        }

        val newArgs: List<String> = args.asList().drop(1)
        val results_: Set<SimulationResult> = builder.perform(System.out, *newArgs.toTypedArray()).get().results
        val results = results_.map { SimulationExperimentResult(it.simArgs, it.resultObject as ExperimentResult) }

        println("")
        for(r in results){
            println("${(r.simArgs.scenario.problemClass as DroneProblemClass).toCSV()};${r.resultObject.toCSV()}")
        }
        results.groupBy { (it.simArgs.scenario.problemClass as DroneProblemClass).protocolType }.mapValues { averageFromResults(it.value) }.forEach {
            println("${it.key} averaged € ${it.value}")
        }
    }

    /**
     * Defines a simple scenario with one depot, one vehicle and three parcels.
     * Note that a scenario is supposed to only contain problem specific
     * information it should (generally) not make any assumptions about the
     * algorithm(s) that are used to solve the problem.
     * @return A newly constructed scenario.
     */
    internal fun createScenario(lastClientAddTime: Long,
                                problem: DroneProblemClass
    ): Scenario {
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
                .problemClass(problem)

        builder = builder.addEvent(AddWarehousesEvent(problem.nbWarehouses))
        for (i in 1..problem.nbDrones) {
            builder = builder.addEvent(AddDroneEvent(problem.chargesInWarehouse, problem.protocolType))
        }
        builder = builder.addEvent(AddClientsEvent(problem.nbInitialClients, problem.nbDynamicClients, lastClientAddTime, problem.protocolType))
        return builder.build()
    }

    internal fun createScenariosForExperiment(scenarioLength: Long, nbDrones: Int): List<Scenario> {
            return (4..10 step 1).flatMap { nbWarehouses ->
                (5..25 step 5).flatMap { nbInitialClients ->
                    (10..50 step 5).flatMap { nbDynamicClients ->
                        listOf(
                                createScenario(scenarioLength, DroneProblemClass(true, ProtocolType.CONTRACT_NET, nbWarehouses, nbDrones, nbInitialClients, nbDynamicClients)),
                                createScenario(scenarioLength, DroneProblemClass(true, ProtocolType.CONTRACT_NET_CONFIRMATION, nbWarehouses, nbDrones, nbInitialClients, nbDynamicClients)),
                                createScenario(scenarioLength, DroneProblemClass(true, ProtocolType.DYNAMIC_CONTRACT_NET, nbWarehouses, nbDrones, nbInitialClients, nbDynamicClients))
                        )
                    }
                }
            }
    }

    internal fun createScenariosForSecondExperiment(scenarioLength: Long, runNumber: Int): List<Scenario> {
        //return (5..100 step 5).flatMap { nbInitialClients ->
        return ((5+runNumber*10)..(10+runNumber*10) step 5).flatMap { nbInitialClients ->
                (10..200 step 5).flatMap { nbDynamicClients ->
                listOf(
                        createScenario(scenarioLength, DroneProblemClass(true, ProtocolType.CONTRACT_NET, 10, 10, nbInitialClients, nbDynamicClients)),
                        createScenario(scenarioLength, DroneProblemClass(true, ProtocolType.CONTRACT_NET_CONFIRMATION, 10, 10, nbInitialClients, nbDynamicClients)),
                        createScenario(scenarioLength, DroneProblemClass(true, ProtocolType.DYNAMIC_CONTRACT_NET, 10, 10, nbInitialClients, nbDynamicClients))
                )
            }
        }
    }
}
