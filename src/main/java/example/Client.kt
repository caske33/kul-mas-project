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

class Client(val position: Point,
             val rng: RandomGenerator,
             val sim: Simulator,
             val protocolType: ProtocolType
) : Depot(position), TickListener, CommUser {

    //private var hasContract: Boolean = false
    //private var messageBroadcast: Boolean = false
    private var device: CommDevice? = null

    var order: Order? = null
      private set
    var drone: Drone? = null
      private set

    private var nextProposalTime: Long = 0

    var nbCallsForProposals: Int = 0
    var nbSwitches: Int = 0

    val state: ClientState
      get() {
          if(order == null)
              return ClientState.LOOKING_FOR_ORDER
          else if(order!!.isDelivered)
              return ClientState.DELIVERED
          else if(order!!.hasExpired)
              return ClientState.OVERTIME
          else if(drone != null){
              if(order!!.isPickedUp)
                  return ClientState.EXECUTING
              else
                  return ClientState.ASSIGNED
          } else {
              return ClientState.AWARDING
          }
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

    override fun tick(timeLapse: TimeLapse) {
        val messages = device?.unreadMessages!!

        // Disagree
        messages.filter { message -> message.contents is Cancel }.forEach { message ->
            if((message.contents as Cancel).bid.order == order)
                cancelContract(message.sender)
            else
                throw IllegalArgumentException("Should disagree on order of this client")
        }
        // Failure
        messages.filter { message -> message.contents is Failure }.forEach { message ->
            cancelContract(message.sender)
        }

        // InformDone
        messages.filter { it.contents is InformDone }.forEach {
            val contents = it.contents as InformDone
            if(contents.order == order) {
                order!!.deliveryTime = contents.deliveryTime
            } else {
                throw IllegalStateException("Received done for order of other client")
            }
        }

        // InformPickedUp
        messages.filter { it.contents is InformPickedUp }.forEach {
            val contents = it.contents as InformPickedUp
            if(contents.order == order) {
                contents.order.pickedUpTime = contents.pickupTime
            }
        }

        // Propose
        if(canNegotiate()){
            val proposeMessages = messages.filter { message -> message.contents is Propose }
            proposeMessages.minBy { message ->
                (message.contents as Propose).bid.bidValue
            }?.let { message ->
                val winningBid = message.contents as Propose
                if(drone != null && message.sender != drone){
                    nbSwitches++
                    device?.send(GotBetterOffer(order!!), drone)
                }
                device?.send(AcceptProposal(winningBid.bid), message.sender)
                proposeMessages.forEach { otherMessage ->
                    if(otherMessage != message){
                        device?.send(RejectProposal((otherMessage.contents as Propose).bid), otherMessage.sender)
                    }
                }
                drone = message.sender as Drone
            }
        }

        //Refuse
        if(drone == null) {
            val refuseMessages = messages.filter { message -> message.contents is Refuse }
            val hasLowRankingRefuse = refuseMessages.any { (it.contents as Refuse).refuseReason == RefuseReason.LOW_RANKING }
            if(refuseMessages.size > 0 && !hasLowRankingRefuse){
                val minProposalTime = refuseMessages.filter { (it.contents as Refuse).refuseReason == RefuseReason.BUSY }.map { (it.contents as Refuse).minimumBusyTime }.min()
                if(minProposalTime != null)
                    nextProposalTime = minProposalTime
            }
        }

        //Send CallForProposal
        if(canNegotiate() && timeLapse.startTime >= nextProposalTime) {
            device?.broadcast(CallForProposal(order!!))
            nbCallsForProposals++
        }

        // ConfirmOrder: neglect, doesn't matter because 100% reliability

        if(timeLapse.startTime > order!!.endTime && (!order!!.isDelivered)){
            order!!.hasExpired = true
        }
    }

    override fun setCommDevice(builder: CommDeviceBuilder?) {
        device = builder!!.setReliability(1.0).build()
    }

    override fun getPosition(): Optional<Point>? {
        return Optional.of(position)
    }

    fun canNegotiate(): Boolean = state.canNegotiate(protocolType)

    private fun cancelContract(sender: CommUser) {
        if(sender == drone){
            drone = null
            order!!.pickedUpTime = -1L
        }
    }
}

enum class ClientState() {
    LOOKING_FOR_ORDER,
    AWARDING,
    ASSIGNED,
    EXECUTING,
    DELIVERED,
    OVERTIME;

    fun canNegotiate(protocolType: ProtocolType): Boolean =
        when(protocolType) {
            ProtocolType.CONTRACT_NET -> this == AWARDING
            ProtocolType.CONTRACT_NET_CONFIRMATION -> this == AWARDING
            ProtocolType.DYNAMIC_CONTRACT_NET -> this == AWARDING || this == ASSIGNED
        }
}