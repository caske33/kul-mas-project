package example.experiment

data class ExperimentResult(val nbCrashes: Int,
                            val totalProfit: Double,
                            val nbClients: Int,
                            val nbClientsDelivered: Int,
                            val nbClientsNotDelivered: Int,
                            val averageDeliveryTime: Double,
                            val nbDrones: Int,
                            val averageDistanceTravelledPerDrone: Double,
                            val averageNbOrdersPerDrone: Double);