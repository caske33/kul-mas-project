package example.experiment

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.road.RoadModel
import example.Drone
import example.Warehouse
import org.apache.commons.math3.random.RandomGenerator

class AddDroneEvent() : DroneWorldEvent(0);

class AddDroneEventHandler() : DroneWorldEventHandler<AddDroneEvent>() {
    override fun handleDroneTimedEvent(event: AddDroneEvent, simulator: Simulator, roadModel: RoadModel, rng: RandomGenerator) {
        simulator.register(Drone(roadModel.getRandomPosition(rng), rng))
    }
}

