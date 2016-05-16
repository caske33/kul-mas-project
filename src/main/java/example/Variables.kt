package example

import com.github.rinde.rinsim.geom.Point

val DRONE_SPEED = 50.0
val DRONE_SPEED_PER_MILLISECOND = DRONE_SPEED / 3600 / 1000
val DISTANCE_PER_PERCENTAGE_BATTERY_DRAIN = 10
//lambda = het verwachte aantal voorvallen per distance unit
//TODO fail more often
val LAMBDA_CRITICAL: Double = 0.1
val LAMBDA_LOW: Double = 0.01
val LAMBDA_NORMAL: Double = 0.0001

val PRICE_DRONE = 10000

val COST_FOR_ENERGY_PER_DISTANCE_UNIT = 2 // in euros

val MIN_WINDOW_LENGTH = 1000000
val MAX_WINDOW_LENGTH = 10000000
val MIN_CLIENT_PRICE = 1.2 // relative tov marketPrice
val MAX_CLIENT_PRICE = 1.3 // relative tov marketPrice
val FINE_PERCENTAGE = 0.5

val BATTERY_CHARGING_RATE = 0.000001 // percentage per time unit

val TICK_LENGTH = 1000L

val MIN_POINT = Point(0.0, 0.0)
val MAX_POINT = Point(10.0, 10.0)
val RANDOM_SEED = 123L
val NUM_DRONES = 5

val TEST_SPEEDUP = 1
val TEST_STOP_TIME = 10 * 60 * 1000.toLong()
val NEW_CUSTOMER_PROB = .03141567841510015464654654654

val NUM_HUBS = 8
val NUM_INITIAL_CLIENTS = 50
