package example.experiment

import com.github.rinde.rinsim.experiment.Experiment

data class SimulationExperimentResult(val simArgs: Experiment.SimArgs, val resultObject: ExperimentResult)

data class ExperimentResult(val nbCrashes: Int,
                            val totalProfit: Double,
                            val nbClients: Int,
                            val nbClientsDelivered: Int,
                            val nbClientsNotDelivered: Int,
                            val averageDeliveryTime: Double,
                            val nbDrones: Int,
                            val averageDistanceTravelledPerDrone: Double,
                            val maximumNbOrdersPerDrone: Int,
                            val estimatedTotalProfit: Double,
                            val estimatedNbCrashes: Double,
                            val nbMessages: Int
){
    fun toCSV(): String {
        return "$nbCrashes;$totalProfit;$nbClients;$nbClientsNotDelivered;$averageDeliveryTime;$nbDrones;$averageDistanceTravelledPerDrone;$maximumNbOrdersPerDrone;$estimatedTotalProfit;$estimatedNbCrashes;$nbMessages"
    }
}

fun aggregateFromResults(results: List<SimulationExperimentResult>, f: (List<Double>) -> Double): AggregateExperimentResult {
  val results_ = results.map { it.resultObject }
  return AggregateExperimentResult(
          results_.map { it.nbCrashes.toDouble() }.let(f),
          results_.map { it.totalProfit }.let(f),
          results_.map { it.nbClients.toDouble() }.let(f),
          results_.map { it.nbClientsDelivered.toDouble() }.let(f),
          results_.map { it.nbClientsNotDelivered.toDouble() }.let(f),
          results_.map { it.averageDeliveryTime }.let(f),
          results_.map { it.nbDrones.toDouble() }.let(f),
          results_.map { it.averageDistanceTravelledPerDrone.toDouble() }.let(f),
          results_.map { it.maximumNbOrdersPerDrone.toDouble() }.let(f),
          results_.map { it.estimatedTotalProfit }.let(f),
          results_.map { it.estimatedNbCrashes }.let(f),
          results_.map { it.nbMessages.toDouble() }.let(f)
  )
}
fun averageFromResults(results: List<SimulationExperimentResult>) = aggregateFromResults(results, List<Double>::average)
fun sumFromResults(results: List<SimulationExperimentResult>) = aggregateFromResults(results, List<Double>::sum)
fun minFromResults(results: List<SimulationExperimentResult>) = aggregateFromResults(results, {it.min()!!})
fun maxFromResults(results: List<SimulationExperimentResult>) = aggregateFromResults(results, {it.max()!!})

data class AggregateExperimentResult(
        val nbCrashes: Double,
        val totalProfit: Double,
        val nbClients: Double,
        val nbClientsDelivered: Double,
        val nbClientsNotDelivered: Double,
        val averageDeliveryTime: Double,
        val nbDrones: Double,
        val averageDistanceTravelledPerDrone: Double,
        val maximumNbOrdersPerDrone: Double,
        val estimatedTotalProfit: Double,
        val estimatedNbCrashes: Double,
        val nbMessages: Double) {
    fun toCSV(): String {
        return "$nbCrashes;$totalProfit;$nbClients;$nbClientsNotDelivered;$averageDeliveryTime;$nbDrones;$averageDistanceTravelledPerDrone;$maximumNbOrdersPerDrone;$estimatedTotalProfit;$estimatedNbCrashes;$nbMessages"
    }
}

