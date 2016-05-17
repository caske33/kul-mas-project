package example.experiment

import com.github.rinde.rinsim.scenario.Scenario

class DroneProblemClass(val chargesInWarehouse: Boolean,
                        val withDynamicContractNet: Boolean
) : Scenario.ProblemClass {
    override fun getId(): String? {
        return "Problem-class. chargesInWarehouse:$chargesInWarehouse, withDynamicContractNet:$withDynamicContractNet"
    }
}