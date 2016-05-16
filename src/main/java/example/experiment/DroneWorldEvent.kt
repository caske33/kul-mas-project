package example.experiment

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.SimulatorAPI
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.scenario.TimedEvent
import com.github.rinde.rinsim.scenario.TimedEventHandler
import org.apache.commons.math3.random.RandomGenerator
import java.io.Serializable

open class DroneWorldEvent(private val time: Long) : TimedEvent {
    override fun getTime(): Long = time
}

abstract class DroneWorldEventHandler<EventType: DroneWorldEvent>() : TimedEventHandler<EventType>, Serializable {
    override fun handleTimedEvent(event: EventType, simulator: SimulatorAPI?) {
        handleDroneTimedEvent(event, simulator as Simulator)
    }

    abstract fun handleDroneTimedEvent(event: EventType,
                                       simulator: Simulator,
                                       roadModel: RoadModel = simulator.modelProvider.getModel(RoadModel::class.java),
                                       rng: RandomGenerator = simulator.randomGenerator)
}
