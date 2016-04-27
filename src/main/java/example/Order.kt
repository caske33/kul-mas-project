package example

class Order(
        val type: PackageType,
        val client : Client,
        val endTime : Long,
        val price : Int,
        val fine : Int
){

    var isPickedUp = false
    var isDelivered = false
    var warehouse: Warehouse? = null
    var drone: Drone? = null

    //val pickupLocation : Point
    //  get() = client.position!!.get()
}
