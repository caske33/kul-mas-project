package example.experiment

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.comm.CommModel
import com.github.rinde.rinsim.core.model.pdp.Vehicle
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.experiment.Experiment.SimArgs
import com.github.rinde.rinsim.experiment.PostProcessor
import example.Client
import example.Drone

class ExperimentPostProcessor() : PostProcessor<ExperimentResult> {

    override fun collectResults(sim: Simulator, args: SimArgs): ExperimentResult {
        val roadModel = sim.modelProvider.getModel(RoadModel::class.java)
        val drones = roadModel.getObjectsOfType(Drone::class.java)
        val clients = roadModel.getObjectsOfType(Client::class.java)

        val commModel = sim.modelProvider.getModel(CommModel::class.java)

        val undeliveredClients = clients.filter { ! it.order!!.isDelivered }
        val totalProfit: Double =
                drones.map { it.totalProfit }.sum() -
                        undeliveredClients.filter { it.order!!.hasExpired }
                               .map { it.order!!.fine }
                               .sum()

        return ExperimentResult(
                drones.filter { it.crashed }.size,
                totalProfit,
                clients.size,
                clients.size - undeliveredClients.size,
                undeliveredClients.size,
                clients.filter { it.order!!.isDelivered }.map { it.order!!.deliveryTime - it.order!!.startTime }.average(),
                drones.size,
                drones.map { it.totalDistanceTravelled }.average(),
                drones.map { it.nbOrdersDelivered }.max()!!,
                drones.map { it.totalEstimatedProfit }.sum() - undeliveredClients.filter { it.order!!.hasExpired }.map { it.order!!.fine } .sum(),
                drones.map { it.totalEstimatedCrashes }.sum(),
                commModel.usersAndDevices.map { it.value.receivedCount }.sum(),
                clients.map { it.nbCallsForProposals }.average(),
                drones.filter { it.crashedByBattery }.size,
                drones.map { it.nbSwitches }.average(),
                clients.map { it.nbSwitches }.average()
        )
    }

    override fun handleFailure(e: Exception, sim: Simulator,
                               args: SimArgs): PostProcessor.FailureStrategy {
        // Signal that when an exception occurs the entire experiment should be
        // aborted.
        return PostProcessor.FailureStrategy.ABORT_EXPERIMENT_RUN
    }
}
