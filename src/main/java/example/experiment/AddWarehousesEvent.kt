package example.experiment

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.geom.Point
import example.Warehouse
import org.apache.commons.math3.random.RandomGenerator

class AddWarehousesEvent(val nbWarehouses: Int) : DroneWorldEvent(0);

class AddWarehouseEventHandler() : DroneWorldEventHandler<AddWarehousesEvent>() {
    override fun handleDroneTimedEvent(event: AddWarehousesEvent, simulator: Simulator, roadModel: RoadModel, rng: RandomGenerator) {
        addWarehouses(event.nbWarehouses, simulator)
    }


    internal fun addWarehouses(nbWarehouses: Int, simulator: Simulator) {
        val roadModel = simulator.modelProvider.getModel(RoadModel::class.java)
        val exponent: Double = Math.floor(Math.log(nbWarehouses.toDouble())/Math.log(2.0))
        val powerOf2SmallerThanNbWarehouses: Int = Math.round(Math.pow(2.0, exponent)).toInt()

        if(nbWarehouses == powerOf2SmallerThanNbWarehouses) {
            val min = roadModel.bounds[0]
            val max = roadModel.bounds[1]
            val totalWidth = max.x - min.x
            val totalHeight = max.y - min.y

            var nbCellsX: Int = Math.round(Math.pow(2.0, Math.floor(exponent/2))).toInt()
            var nbCellsY: Int = if (Math.round(exponent) % 2 == 0L) nbCellsX  else 2*nbCellsX
            val cellWidth = totalWidth / nbCellsX
            val cellHeight = totalHeight / nbCellsY

            for(i in 0..nbCellsX - 1) {
                for(j in 0..nbCellsY - 1) {
                    addWarehouse(simulator, min.x + i * cellWidth, min.y + j * cellHeight, cellWidth, cellHeight)
                }
            }
            //addWarehouse(simulator, roadModel.bounds[0], roadModel.bounds[1])
        } else {
            addWarehouses(powerOf2SmallerThanNbWarehouses, simulator)
            addWarehouses(nbWarehouses - powerOf2SmallerThanNbWarehouses, simulator)
        }
    }

    internal fun addWarehouse(simulator: Simulator, minX: Double, minY: Double, width: Double, height: Double) {
        val rng = simulator.randomGenerator
        val randomPoint = Point(minX + rng.nextDouble() * width, minY + rng.nextDouble() * height);
        simulator.register(Warehouse(randomPoint, rng))
    }

}

