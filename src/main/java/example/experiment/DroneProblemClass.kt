package example.experiment

import com.github.rinde.rinsim.scenario.Scenario
import example.ProtocolType

data class DroneProblemClass(val chargesInWarehouse: Boolean,
                        val protocolType: ProtocolType
) : Scenario.ProblemClass {
    override fun getId(): String? {
        return "Problem-class. chargesInWarehouse:$chargesInWarehouse, protocol:$protocolType"
    }
}