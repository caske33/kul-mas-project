package example

class Order(
        val type: PackageType,
        val client: Client,
        val startTime: Long,
        val endTime : Long,
        val price : Double,
        val fine : Double
){
    var hasExpired = false
    var warehouse: Warehouse? = null
    var drone: Drone? = null

    var deliveryTime: Long = -1
    val isDelivered: Boolean
        get() = deliveryTime >= 0 && deliveryTime <= endTime
}
