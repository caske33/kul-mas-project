package example

import com.github.rinde.rinsim.core.Simulator
import com.github.rinde.rinsim.core.model.comm.CommDevice
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder
import com.github.rinde.rinsim.core.model.comm.CommUser
import com.github.rinde.rinsim.core.model.pdp.Depot
import com.github.rinde.rinsim.core.model.pdp.PDPModel
import com.github.rinde.rinsim.core.model.pdp.Parcel
import com.github.rinde.rinsim.core.model.road.RoadModel
import com.github.rinde.rinsim.core.model.time.TickListener
import com.github.rinde.rinsim.core.model.time.TimeLapse
import com.github.rinde.rinsim.geom.Point
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import org.apache.commons.math3.random.RandomGenerator
import java.text.FieldPosition

class Client(val position: Point, val rng: RandomGenerator, val sim: Simulator, val usesDynamicContractNet: Boolean) : Depot(position), TickListener, CommUser {

    //private var hasContract: Boolean = false
    //private var messageBroadcast: Boolean = false
    private var device: CommDevice? = null

    var order: Order? = null
      private set
    var drone: Drone? = null
      private set

    val state: ClientState
      get() {
          if(order == null)
              return ClientState.LOOKING_FOR_ORDER

          if(order!!.isDelivered)
              return ClientState.DELIVERED
          if(order!!.hasExpired)
              return ClientState.OVERTIME

          if(drone != null){
              if(drone!!.canNegotiate())
                  return ClientState.ASSIGNED
              else
                  return ClientState.EXECUTING
          }

          return ClientState.AWARDING
      }

    override fun initRoadPDP(roadModel: RoadModel?, pdpModel: PDPModel?) {
        super.initRoadPDP(roadModel, pdpModel)

        val type = PackageType.values()[rng.nextInt(PackageType.values().size)]
        val percent = rng.nextDouble()
        val windowLength = MIN_WINDOW_LENGTH + (MAX_WINDOW_LENGTH-MIN_WINDOW_LENGTH) * percent
        val price = (MAX_CLIENT_PRICE - (MAX_CLIENT_PRICE-MIN_CLIENT_PRICE)*percent)*type.marketPrice
        val fine = FINE_PERCENTAGE * type.marketPrice

        order = Order(type, this, sim.currentTime, Math.round(sim.currentTime + windowLength), price, fine)
    }

    override fun afterTick(timeLapse: TimeLapse?) {
        //
    }

    override fun tick(timeLapse: TimeLapse?) {
        val messages = device?.unreadMessages!!

        // CancelOrder
        messages.filter { message -> message.contents is CancelOrder  || message.contents is DroneCrashMessage }.forEach { message ->
            if(message.sender == drone)
                drone = null
        }

        // BidOnOrder
        if(canNegotiate()){
            messages.filter { message -> message.contents is BidOnOrder }.minBy { message ->
                (message.contents as BidOnOrder).bid.bidValue
            }?.let { message ->
                val winningBid = message.contents as BidOnOrder
                if(drone != null && message.sender != drone){
                    device?.send(GotBetterOffer(order!!), drone)
                }
                device?.send(AcceptOrder(winningBid.bid), message.sender)
                drone = message.sender as Drone
            }
        }

        //Send DeclareOrder
        if(canNegotiate()) {
            device?.broadcast(DeclareOrder(order!!))
        }

        // ConfirmOrder: neglect, doesn't matter because 100% reliability

        if(timeLapse!!.startTime > order!!.endTime && (!order!!.isDelivered)){
            order!!.hasExpired = true
        }
    }

    override fun setCommDevice(builder: CommDeviceBuilder?) {
        device = builder!!.setReliability(1.0).build()
    }

    override fun getPosition(): Optional<Point>? {
        return Optional.of(position)
    }

    fun deliverOrder(deliverTime: Long, order: Order): Double {
        if(order == this.order){
            order.deliveryTime = deliverTime;

            if(deliverTime < order.endTime)
                return order.price
            else
                return -order.fine
        }
        throw IllegalArgumentException("should pass order of the client")
    }

    fun canNegotiate(): Boolean = state.canNegotiate(usesDynamicContractNet)
}

enum class ClientState(val canNegotiate: Boolean,
                       val canNegotiateDynamic: Boolean = canNegotiate) {
    LOOKING_FOR_ORDER(false),
    AWARDING(true),
    ASSIGNED(false, true),
    EXECUTING(false),
    DELIVERED(false),
    OVERTIME(false);

    fun canNegotiate(dynamic: Boolean) = if(dynamic) canNegotiateDynamic else canNegotiate
}