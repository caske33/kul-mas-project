package example

class Order(
        val type: PackageType,
        val client : Client,
        val endTime : Long,
        val price : Double,
        val fine : Double
){

    var isPickedUp = false
    var isDelivered = false
    var warehouse: Warehouse? = null
    var drone: Drone? = null
}
