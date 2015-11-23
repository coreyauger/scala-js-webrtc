package io.surfkit.clientlib.webrtc

import java.util.UUID
import io.surfkit.clientlib.webrtc.Peer.PeerInfo
import org.scalajs.dom
import scala.concurrent.Promise
import scala.util.Try
import scalajs.js
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.raw.{DOMError, Event}


object Peer{
  case class Props(id:String,
                   signaler: PeerSignaler,
                   rtcConfiguration:RTCConfiguration,
                   receiveMedia: MediaConstraints,
                   peerConnectionConstraints: MediaConstraints,
                   `type`:String = "video",
                   oneway:Boolean = false,
                   sharemyscreen:Boolean = false,
                   browserPrefix:Option[String] = None,
                   sid:String = UUID.randomUUID().toString
                    )

  sealed trait Signaling{
    def  peer:PeerInfo
  }

  case class PeerInfo(id:String, `type`: String)

  case class Join(room:String, peer:PeerInfo) extends Signaling
  case class Room(name:String, peer:PeerInfo, config:RTCConfiguration, members:js.Array[PeerInfo]) extends Signaling

  case class Offer(peer:PeerInfo, offer:RTCSessionDescription) extends Signaling
  case class Answer(peer:PeerInfo, answer:RTCSessionDescription) extends Signaling
  case class Candidate(peer:PeerInfo, candidate:RTCIceCandidate) extends Signaling
  case class Error(peer:PeerInfo, reason:String) extends Signaling

  trait PeerSignaler{
    def send(s:Signaling):Unit
  }

  trait ModelTransformPeerSignaler[T] extends PeerSignaler{
    val peerInfo:PeerInfo

    var receivers = js.Array[(Signaling) => Unit]()

    def toPeerSignaling(model:T):Signaling
    def fromPeerSignaling(s:Signaling):T

    def receive(s:Signaling) = receivers.foreach(_.apply(s))
    override def send(s:Signaling):Unit = sendModel(fromPeerSignaling(s))

    // override ME
    def sendModel(model:T):Unit

  }
}

/**
 * Created by corey auger on 14/11/15.
 */
class Peer(p:Peer.Props) {

  val id = p.id
  val sid = p.sid

  val info = PeerInfo(id, p.`type`)

  private val streamPromise = Promise[MediaStream]()
  val stream = streamPromise.future

  val pc = new RTCPeerConnection(p.rtcConfiguration, p.peerConnectionConstraints)
  val addStream = pc.addStream _
  val removeStream = pc.removeStream _

  pc.onaddstream = { evt: MediaStreamEvent =>
    streamPromise.complete(Try(evt.stream))
    onAddStream(evt.stream)
  }

  // override me
  def streamAdded(stream:MediaStream): Unit ={}

  var onAddStream = streamAdded _

  pc.onicecandidate = { evt:RTCPeerConnectionIceEvent =>
    if( evt.candidate != null)  // FIXME: really ?
      p.signaler.send(Peer.Candidate(info, evt.candidate))
  }
  pc.onnegotiationneeded = { evt:Event =>
    println("onNegotiationneeded")
  }
  pc.oniceconnectionstatechange = { evt:Event =>
    println("oniceconnectionstatechange")
    pc.iceConnectionState match {
      case IceConnectionState.failed =>
        // currently, in chrome only the initiator goes to failed
        // so we need to signal this to the peer
        if (pc.localDescription.`type` == RTCSdpType.offer) {
          println("iceFailed ")
          p.signaler.send( Peer.Error(info, "connectivityError ICE FAILED"))
        }
    }
  }
  pc.onsignalingstatechange = { evt:Event =>
    println(s"onsignalingstatechange: ${pc.signalingState}")
  }


  def start():Unit = {
    // well, the webrtc api requires that we either
    // a) create a datachannel a priori
    // b) do a renegotiation later to add the SCTP m-line
    // Let's do (a) first...
    // TODO: ...
    //if (enableDataChannels) {
    //  getDataChannel('simplewebrtc');
    //}
    println("create offer")
    pc.createOffer({ offer:RTCSessionDescription =>
      // does not work for jingle, but jingle.js doesn't need
      // this hack...
      val expandedOffer =  RTCSessionDescription(`type` = "offer", sdp = offer.sdp)
      //println(s"offer: ${offer}")
      println("setLocalDescription")
      pc.setLocalDescription(offer,() =>{
        println("signal offer")
        p.signaler.send(Peer.Offer(info, offer))
      },handleError _)
    },handleError _,p.receiveMedia)
  }

  def handleError(err:DOMError):Unit = {
    println("ERROR")
    println(err)
  }

  def end = {
    pc.close
  }


  def handleMessage(message:Peer.Signaling):Unit = {
    println(s"handleMessage ${message.toString}")

    //if (message.prefix) this.browserPrefix = message.prefix;
    message match{
      case Peer.Offer(peer, offer) if peer.id == id =>
        //println(s"Offer ${offer.toString}")
        println("Peer.Offer")
        pc.setRemoteDescription(offer,() => {
          println("setRemoteDescription success")
          // auto-accept
          println("creating an answer..")
          pc.createAnswer({ answer:RTCSessionDescription =>
            println(s"createAnswer: ${answer}")
            p.signaler.send(Peer.Answer(info, answer))
          },handleError _, p.receiveMedia)
        },handleError _)

      case Peer.Answer(peer, answer) if peer.id == id =>
        println("Peer.Answer")
        pc.setRemoteDescription(answer, () => {
          println("setRemoteDescription. success")
          // SEE   if (self.wtFirefox) { .. }  https://github.com/otalk/RTCPeerConnection/blob/master/rtcpeerconnection.js#L507
        },handleError _)

      case Peer.Candidate(peer, candidate) if peer.id == id =>
        println(s"Peer.Candidate ${candidate.toString}")
        pc.addIceCandidate(candidate, () => {
          println("addIceCandidate. success")
        },handleError _)

      case Peer.Error(peer, reason) if peer.id == id =>
        println(s"Peer sent you error: ${reason}")

      case _ =>
        println(s"Unkown peer handleMessage ${message}")
    }
  }

}
