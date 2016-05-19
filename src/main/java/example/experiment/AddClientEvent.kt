package example.experiment

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import example.Client
import example.Drone
import example.ProtocolType
import example.Warehouse
import org.apache.commons.math3.random.RandomGenerator

class AddClientsEvent(val nbClients: Int,
                      val maxClientTime: Long,
                      val protocolType: ProtocolType
) : DroneWorldEvent(0);

class AddClientsEventHandler() : DroneWorldEventHandler<AddClientsEvent>() {
    override fun handleDroneTimedEvent(event: AddClientsEvent, simulator: Simulator, roadModel: RoadModel, rng: RandomGenerator) {
        val clientTimes = (1..event.nbClients).map {
            Math.round(rng.nextDouble() * event.maxClientTime)
        }
        simulator.register(object : TickListener {
            override fun tick(timeLapse: TimeLapse) {
                clientTimes.filter { time ->
                    timeLapse.startTime <= time && time <= timeLapse.endTime
                }.forEach {
                    simulator.register(Client(roadModel.getRandomPosition(rng), rng, simulator, event.protocolType == ProtocolType.DYNAMIC_CONTRACT_NET))
                }
            }

            override fun afterTick(timeLapse: TimeLapse?) {
            }
        })
    }
}

