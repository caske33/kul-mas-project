package example

import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.pdp.Vehicle
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO
import com.github.rinde.rinsim.core.model.road.MovingRoadUser
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional

/**
 * Created by victo on 19/04/2016.
 */
class Plane(var position: Point) : Vehicle(VehicleDTO.builder().capacity(350).startPosition(position).speed(Plane.SPEED).build()), MovingRoadUser, TickListener, CommUser {
    override fun setCommDevice(builder: CommDeviceBuilder?) {
        throw UnsupportedOperationException()
    }

    override fun getPosition(): Optional<Point>? {
        throw UnsupportedOperationException()
    }

    override fun tickImpl(time: TimeLapse?) {
        throw UnsupportedOperationException()
    }

    companion object {
        private val SPEED = 500.0
    }
}
