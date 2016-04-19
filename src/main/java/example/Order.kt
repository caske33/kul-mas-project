package example

import com.github.rinde.rinsim.core.model.pdp.Parcel
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import java.util.*

class Order(
        dto : ParcelDTO,
        var content : PackageType,
        var origin : Hub
) : Parcel(dto), TickListener {
    // origin = Hub
    // pickupLocation = Hub position
    // deliveryLocation = Client

    var isPickedUp = false
    var isDelivered = false

    override fun tick(timeLapse: TimeLapse?) {
        //
    }

    override fun afterTick(timeLapse: TimeLapse?) {
        //
    }

    fun getPosition(): Optional<Point>? {
        val rm = roadModel
        if (rm.containsObject(this)) {
            return Optional.of(rm.getPosition(this))
        }
        return Optional.empty()
    }
}
