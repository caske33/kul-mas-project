package example.experiment

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.road.RoadModel
import example.Client
import example.Drone
import example.Warehouse
import org.apache.commons.math3.random.RandomGenerator

class AddClientEvent(time: Long,
                     val withDynamicContractNet: Boolean
) : DroneWorldEvent(time);

class AddClientEventHandler() : DroneWorldEventHandler<AddClientEvent>() {
    override fun handleDroneTimedEvent(event: AddClientEvent, simulator: Simulator, roadModel: RoadModel, rng: RandomGenerator) {
        simulator.register(Client(roadModel.getRandomPosition(rng), rng, simulator, event.withDynamicContractNet))
    }
}

