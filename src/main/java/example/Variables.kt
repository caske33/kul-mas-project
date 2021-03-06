package example

import com.github.rinde.rinsim.geom.Point

val DRONE_SPEED = 50.0
val DRONE_SPEED_PER_MILLISECOND = DRONE_SPEED / 3600 / 1000
val DISTANCE_PER_FULL_BATTERY = 15
//lambda = het verwachte aantal voorvallen per distance unit
val LAMBDA_CRITICAL: Double = 0.1
val LAMBDA_LOW: Double = 0.01
val LAMBDA_NORMAL: Double = 0.0001

val PRICE_DRONE = 3000

val COST_FOR_ENERGY_PER_DISTANCE_UNIT = 2 // in euros

val MIN_WINDOW_LENGTH = 1000 * 1000
val MAX_WINDOW_LENGTH = 5000 * 1000
val MIN_CLIENT_PRICE = 1.2 // relative tov marketPrice
val MAX_CLIENT_PRICE = 1.3 // relative tov marketPrice
val FINE_PERCENTAGE = 0.5 // percentage of marketPrice for fine

val BATTERY_CHARGING_RATE = 0.000002 // battery per millisecond

val MIN_POINT = Point(0.0, 0.0)
val MAX_POINT = Point(10.0, 10.0)
val RANDOM_SEED = 1234L
