package io.surfkit.clientlib.webrtc

import java.util.UUID
import io.surfkit.clientlib.webrtc.Peer.PeerInfo
import org.scalajs.dom
import org.scalajs.dom._
import scala.util.Try
import scalajs.js
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.raw.{DOMError, Event}
import scala.concurrent.ExecutionContext.Implicits.global


object Peer{
  case class Props(local:PeerInfo,
                   remote:PeerInfo,
                   signaler: PeerSignaler,
                   rtcConfiguration:RTCConfiguration,
                   `type`:String = "video",
                   oneway:Boolean = false,
                   sharemyscreen:Boolean = false,
                   browserPrefix:Option[String] = None,
                   sid:String = UUID.randomUUID().toString
                    )

  sealed trait Signaling{
    def remote:PeerInfo
    def local:PeerInfo
  }

  case class PeerInfo(id:String, `type`: String)
  def EmptyPeer = PeerInfo("","")

  case class Join(remote: PeerInfo, local: PeerInfo, room:String) extends Signaling
  case class Room(remote: PeerInfo, local: PeerInfo, name:String, members:js.Array[PeerInfo]) extends Signaling

  case class Offer(remote: PeerInfo, local: PeerInfo, offer:RTCSessionDescription) extends Signaling
  case class Answer(remote: PeerInfo, local: PeerInfo, answer:RTCSessionDescription) extends Signaling
  case class Candidate(remote: PeerInfo, local: PeerInfo, candidate:RTCIceCandidate) extends Signaling
  case class Error(remote: PeerInfo, local: PeerInfo, reason:String) extends Signaling

  trait PeerSignaler{
    def send(s:Signaling):Unit
  }

  trait ModelTransformPeerSignaler[T] extends PeerSignaler{
    val localPeer:PeerInfo

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

  val local = p.local
  val remote = p.remote
  val sid = p.sid

  var streams = List.empty[MediaStream]

  val pc = new RTCPeerConnection(p.rtcConfiguration)
  val addStream = pc.addStream _
  val removeStream = pc.removeStream _


  pc.onaddstream = { evt: MediaStreamEvent =>
    println("onaddstream")
    debug
    evt.stream.getTracks.foreach{ t:MediaStreamTrack =>
      t.oneended = { ev:Event =>
        println("Track oneended")
        onRemoveStream(evt.stream)
      }
    }
    streams = evt.stream :: streams
    onAddStream(evt.stream)
  }

  pc.onremovestream = { evt: MediaStreamEvent =>
    onRemoveStream(evt.stream)
  }

  // Override these event handlers.
  var onAddStream = (stream:MediaStream) => {}
  var onRemoveStream = (stream:MediaStream) => {}

  pc.onicecandidate = { evt:RTCPeerConnectionIceEvent =>
    println("[INTO] - onicecandidate")
    println(s"[INFO] - ice gathering state ${pc.iceGatheringState}")
    if( evt.candidate != null)  // FIXME: really ?
      p.signaler.send(Peer.Candidate(remote,local, evt.candidate))
    else
      println("[WARN] - there was a NULL for candidate")
    debug
  }
  pc.onnegotiationneeded = { evt:Event =>
    println("onNegotiationneeded")
    debug
  }
  pc.oniceconnectionstatechange = { evt:Event =>
    println(s"[INFO] - ice gathering state ${pc.iceGatheringState}")
    println("oniceconnectionstatechange")
    pc.iceConnectionState match {
      case IceConnectionState.failed =>
        // currently, in chrome only the initiator goes to failed
        // so we need to signal this to the peer
        if (pc.localDescription.`type` == RTCSdpType.offer) {
          println("iceFailed ")
          p.signaler.send( Peer.Error(remote, local, "connectivityError ICE FAILED"))
        }
      case IceConnectionState.disconnected =>
        println("[ERROR] - IceConnectionState.disconnected")
        streams.headOption.foreach(onRemoveStream)

      case allOther =>
        println(s"IceConnectionState ${allOther}")
        debug
    }
  }
  pc.onsignalingstatechange = { evt:Event =>
    debug
  }

  def debug = {
    println(s"=======================================================================")
    println(s"[INFO] - iceConnectionState:      ${pc.iceConnectionState}")
    println(s"[INFO] - onsignalingstatechange:  ${pc.signalingState}")
    println(s"[INFO] - ice gathering state      ${pc.iceGatheringState}")
    println(s"=======================================================================")
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
    pc.createOffer().andThen({ offer:RTCSessionDescription =>
      val expandedOffer =  RTCSessionDescription(`type` = "offer", sdp = offer.sdp)
      //println(s"offer: ${offer}")
      println("setLocalDescription")
      pc.setLocalDescription(offer).andThen({ x:Any =>
        println("signal offer")
        p.signaler.send(Peer.Offer(remote, local, offer))
        debug
      },handleError _)
    },handleError _)
  }

  def handleError(err:Any):Unit = {
    println(s"[ERROR] - ${err}")
  }

  def end = {
    pc.close
  }

  def answer(offer:RTCSessionDescription) = {
    println("creating an answer..")
    pc.createAnswer().andThen({ answer:RTCSessionDescription =>
      pc.setLocalDescription(answer).andThen({ x:Any =>
        println(s"createAnswer for:  ${remote}")
        p.signaler.send(Peer.Answer(remote, local, answer))
        debug
      },handleError _)

    },handleError _)
  }


  def handleMessage(message:Peer.Signaling):Unit = {
    //println(s"handleMessage ${message.toString}")

    //if (message.prefix) this.browserPrefix = message.prefix;
    message match{
      case Peer.Offer(r, l, offer) if l.id == remote.id =>
        //println(s"Offer ${offer.toString}")
        println(s"Peer.Offer from: ${l}")
        pc.setRemoteDescription(offer).andThen({ x:Any =>
          println("setRemoteDescription success")
          println("CALLING answer")
          // auto-accept
          answer(offer)
          debug
        },handleError _)

      case Peer.Answer(r, l, answer) if l.id == remote.id =>
        println(s"Peer.Answer from: ${l}")
        pc.setRemoteDescription(answer).andThen({ x:Any =>
          println("setRemoteDescription. success")
          debug
          // SEE   if (self.wtFirefox) { .. }  https://github.com/otalk/RTCPeerConnection/blob/master/rtcpeerconnection.js#L507
        },handleError _)

      case Peer.Candidate(r, l, candidate) if l.id == remote.id =>
        println(s"Peer.Candidate ${candidate.toString}")
        pc.addIceCandidate(candidate).andThen({ x:Any =>
          println("addIceCandidate. success")
          debug
        },handleError _)

      case Peer.Error(r, l, reason) if l.id == remote.id =>
        println(s"[ERROR] - Peer sent you error: ${reason}")

      case _ =>
        println(s"[WARN] - Unkown peer handleMessage ${message}")
    }
  }

}
