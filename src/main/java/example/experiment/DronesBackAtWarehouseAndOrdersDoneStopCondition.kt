package example.experiment

import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.core.model.time.Clock
import com.github.rinde.rinsim.scenario.StopCondition
import com.google.common.collect.ImmutableSet
import example.Client
import example.Drone
import example.Warehouse

class DronesBackAtWarehouseAndOrdersDoneStopCondition(val lastClientTime: Long) : StopCondition {

    override fun getTypes(): ImmutableSet<Class<*>>? {
        return ImmutableSet.of(Clock::class.java, RoadModel::class.java)
    }

    override fun evaluate(provider: StopCondition.TypeProvider?): Boolean {
        val now = provider!!.get(Clock::class.java).currentTime


        val roadModel = provider.get(RoadModel::class.java)

        val nbDronesNotInWarehouse = roadModel.getObjectsOfType(Drone::class.java).filter { drone ->
            roadModel.getObjectsAt(drone, Warehouse::class.java).size == 0
        }.size

        val nbDronesNotFullyCharged = roadModel.getObjectsOfType(Drone::class.java).filterNot {
            it.batteryLevel == 1.0 || it.crashed
        }.size


        val nbClientsNotDeliveredOrExpired = roadModel.getObjectsOfType(Client::class.java).filterNot { client ->
            client.order!!.hasExpired || client.order!!.isDelivered
        }.size

        if(now < lastClientTime || nbDronesNotFullyCharged > 0 || nbClientsNotDeliveredOrExpired > 0)
            return false

        return true
    }

}