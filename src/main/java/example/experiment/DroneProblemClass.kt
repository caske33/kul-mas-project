package example.experiment

import com.github.rinde.rinsim.scenario.Scenario
import example.ProtocolType

data class DroneProblemClass(val chargesInWarehouse: Boolean,
                             val protocolType: ProtocolType,
                             val nbWarehouses: Int,
                             val nbDrones: Int,
                             val nbInitialClients: Int,
                             val nbDynamicClients: Int
) : Scenario.ProblemClass {
    override fun getId(): String? {
        return "Problem-class. chargesInWarehouse:$chargesInWarehouse, protocol:$protocolType"
    }

    fun toCSV(): String {
        return "$chargesInWarehouse;$protocolType;$nbWarehouses;$nbDrones;$nbInitialClients;$nbDynamicClients"
    }
}