package example

val DRONE_SPEED = 50.0
val DISTANCE_PER_PERCENTAGE_BATTERY_DRAIN = 10
//lambda = het verwachte aantal voorvallen per distance unit
//TODO fail more often
val LAMBDA_CRITICAL = 1/100
val LAMBDA_LOW = 1/1000
val LAMBDA_NORMAL = 1/100000

val PRICE_DRONE = 10000

val COST_FOR_ENERGY_PER_DISTANCE_UNIT = 2 // in euros

val MIN_WINDOW_LENGTH = 1000000
val MAX_WINDOW_LENGTH = 10000000
val MIN_CLIENT_PRICE = 1.2 // relative tov marketPrice
val MAX_CLIENT_PRICE = 1.3 // relative tov marketPrice
val FINE_PERCENTAGE = 0.5

val BATTERY_CHARGING_RATE = 0.000001 // percentage per time unit

val TICK_LENGTH = 1000L
