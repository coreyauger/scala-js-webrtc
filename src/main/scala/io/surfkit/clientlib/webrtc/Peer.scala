package io.surfkit.clientlib.webrtc

import java.util.UUID
import io.surfkit.clientlib.webrtc.Peer.PeerInfo
import org.scalajs.dom
import org.scalajs.dom._
import scala.scalajs.js.annotation.JSExportAll
import scala.util.Try
import scalajs.js
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.experimental.mediastream._
import org.scalajs.dom.raw.{DOMError, Event}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.|

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

  case class Offer(remote: PeerInfo, local: PeerInfo, offer: RTCSessionDescription, room: String) extends Signaling
  case class Answer(remote: PeerInfo, local: PeerInfo, answer: RTCSessionDescription) extends Signaling
  case class Candidate(remote: PeerInfo, local: PeerInfo, candidate:RTCIceCandidate) extends Signaling
  case class Error(remote: PeerInfo, local: PeerInfo, reason:String) extends Signaling

  trait PeerSignaler{
    def send(s:Signaling):Unit
  }

  trait ModelTransformPeerSignaler[T] extends PeerSignaler{
    var localPeer:PeerInfo

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

  var canSendIce = false
  var iceBuffer = List.empty[RTCIceCandidate]

  val pc = new RTCPeerConnection(p.rtcConfiguration)
  val addStream = pc.addStream _
  val removeStream = pc.removeStream _


  pc.onaddstream = { evt: MediaStreamEvent =>
    println("onaddstream")
    val tracks = evt.stream.getAudioTracks.concat(evt.stream.getVideoTracks)
    tracks.foreach{ t:MediaStreamTrack =>
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
  var onIceConnectionStateChange: (RTCIceConnectionState) => Unit = { s => }
  var onSignalingStateChange: (RTCSignalingState) => Unit = { s => }

  pc.onicecandidate = { evt:RTCPeerConnectionIceEvent =>
    if( evt.candidate != null) {
      if(canSendIce) {
        println(s"Sending candidate ${evt.candidate}")
        p.signaler.send(Peer.Candidate(remote, local, evt.candidate))
      }else{
        iceBuffer = evt.candidate :: iceBuffer
      }
    }else {
      println("[WARN] - there was a NULL for candidate")
    }
  }
  pc.onnegotiationneeded = { evt:Event =>
    println("onNegotiationneeded")
  }
  pc.oniceconnectionstatechange = { evt:Event =>
    println("oniceconnectionstatechange")
    pc.iceConnectionState match {
      case RTCIceConnectionState.failed =>
        // currently, in chrome only the initiator goes to failed
        // so we need to signal this to the peer
        if (pc.localDescription.`type` == RTCSdpType.offer) {
          println("iceFailed ")
          p.signaler.send( Peer.Error(remote, local, "connectivityError ICE FAILED"))
        }
      case RTCIceConnectionState.disconnected =>
        println("[ERROR] - IceConnectionState.disconnected")
        streams.headOption.foreach(onRemoveStream)

      case allOther =>
        println(s"IceConnectionState ${allOther}")

    }
    onIceConnectionStateChange( pc.iceConnectionState )
  }
  pc.onsignalingstatechange = { evt:Event =>
    debug
    onSignalingStateChange( pc.signalingState )
  }

  def debug = {
    //println(s"=======================================================================")
    println(s"[INFO] - iceConnectionState:      ${pc.iceConnectionState}")
    println(s"[INFO] - onsignalingstatechange:  ${pc.signalingState}")
    println(s"[INFO] - ice gathering state      ${pc.iceGatheringState}")
    println(s"=======================================================================")
  }

  def iceConnectionState = pc.iceConnectionState
  def signalingState = pc.signalingState

  def start(room: String):Unit = {
    println(s"create offer for room: ${room}")
    pc.createOffer().toFuture.map{ offer:RTCSessionDescription =>
      val expandedOffer =  new RTCSessionDescription(RTCSessionDescriptionInit(`type` = RTCSdpType.offer, sdp = offer.sdp))
      //println(s"offer: ${offer}")
      println("setLocalDescription")
      pc.setLocalDescription(expandedOffer).toFuture.map{ x:Any =>
        println("signal offer")
        p.signaler.send(Peer.Offer(remote, local, expandedOffer, room))
      }.recover(reportFailure)
    }.recover(reportFailure)
  }

/*  def handleError(err:Any):Unit = {
    println(s"[ERROR] - ${err}")
  }*/
  val reportFailure:scala.PartialFunction[scala.Throwable, Unit] = {
    case t:Throwable =>
      t.printStackTrace()
      println("[ERROR] - An error has occured: " + t)
  }

//
  def end = {
    canSendIce = false
    streams.foreach(_.getVideoTracks.foreach(_.stop))
    streams.foreach(_.getAudioTracks.foreach(_.stop))
    iceBuffer = List.empty[RTCIceCandidate]
    pc.close
  }

  def drainIce = {
    canSendIce = true
    iceBuffer.foreach(i => p.signaler.send(Peer.Candidate(remote, local, i)) )
    iceBuffer = List.empty[RTCIceCandidate]
  }

  def answer = {
    println("creating an answer..")
    pc.createAnswer().toFuture.map{ answer:RTCSessionDescription =>
      pc.setLocalDescription(answer).toFuture.map{ x:Any =>
        println(s"createAnswer for:  ${remote}")
        p.signaler.send(Peer.Answer(remote, local, answer))
      }.recover(reportFailure)
    }.recover(reportFailure)
  }

  def handleMessage(message:Peer.Signaling):Unit =
    message match{
      case Peer.Offer(r, l, offer, room) if l.id == remote.id =>
        //println(s"Offer ${offer.toString}")
        println(s"Peer.Offer from: ${l}")
        pc.setRemoteDescription(offer).toFuture.map{ x:Any =>
          println("setRemoteDescription success")
          answer
          drainIce
        }.recover(reportFailure)

      case Peer.Answer(r, l, answer) if l.id == remote.id =>
        println(s"Peer.Answer from: ${l}")
        pc.setRemoteDescription(answer).toFuture.map{ x:Any =>
          println("setRemoteDescription. success")
          drainIce
        }.recover(reportFailure)

      case Peer.Candidate(r, l, candidate) if l.id == remote.id =>
        pc.addIceCandidate(candidate).toFuture.map{ x:Any =>
          println("addIceCandidate. success")
        }.recover(reportFailure)

      case Peer.Error(r, l, reason) if l.id == remote.id =>
        println(s"[ERROR] - Peer sent you error: ${reason}")

      case _ =>
        // println(s"[WARN] - Unknown peer handleMessage ${message}")
        // Just ignore...
    }

}
