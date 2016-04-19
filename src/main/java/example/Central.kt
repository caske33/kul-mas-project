package example

import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.pdp.Depot
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional

class Central(location: Point) : Depot(location), TickListener, CommUser {
    override fun afterTick(timeLapse: TimeLapse?) {
        throw UnsupportedOperationException()
    }

    override fun getPosition(): Optional<Point> {
        throw UnsupportedOperationException()
    }

    override fun setCommDevice(builder: CommDeviceBuilder?) {
        throw UnsupportedOperationException()
    }

    override fun tick(timeLapse: TimeLapse?) {
        throw UnsupportedOperationException()
    }
}
