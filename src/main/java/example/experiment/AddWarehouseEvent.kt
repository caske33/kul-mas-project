package example.experiment

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.road.RoadModel
import example.Warehouse
import org.apache.commons.math3.random.RandomGenerator

class AddWarehouseEvent() : DroneWorldEvent(0);

class AddWarehouseEventHandler() : DroneWorldEventHandler<AddWarehouseEvent>() {
    override fun handleDroneTimedEvent(event: AddWarehouseEvent, simulator: Simulator, roadModel: RoadModel, rng: RandomGenerator) {
        simulator.register(Warehouse(roadModel.getRandomPosition(rng), rng))
    }
}

