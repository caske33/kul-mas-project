package example

import com.github.rinde.rinsim.core.model.comm.CommDevice
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.pdp.Depot
import com.github.rinde.rinsim.core.model.pdp.PDPModel
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import org.apache.commons.math3.random.RandomGenerator

class Warehouse(val position: Point, val rng: RandomGenerator) : Depot(position), TickListener {

    var device: CommDevice? = null

    var hasClientContract = false
    var pickedUp = false
    var messageBroadcast = false

    val prices: Map<PackageType, Double>

    init {
        prices = PackageType.values().associate { type ->
            val price = type.marketPrice * (1 + rng.nextDouble()*0.4-0.2) // +/- 20% of market price
            Pair(type, price)
        }
    }

    override fun initRoadPDP(pRoadModel: RoadModel?, pPdpModel: PDPModel?) {
    }

    fun getPriceFor(type: PackageType): Double
        = prices.get(type)!!

    override fun tick(timeLapse: TimeLapse) {
    }

    override fun afterTick(timeLapse: TimeLapse) {
    }
}
