package example

class Bid(val order: Order,
          val bidValue: Double,
          val warehouse: Warehouse,
          val estimatedProbabilityFailure: Double
)