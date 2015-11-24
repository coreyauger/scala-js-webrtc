package io.surfkit.clientlib.webrtc

import java.util.UUID

import io.surfkit.clientlib.webrtc.Peer.PeerInfo
import org.scalajs.dom._
import scala.concurrent.{Promise, Future}
import scala.scalajs.js
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import org.scalajs.dom.{MediaStream, MediaStreamTrack, MediaStreamEvent}

/**
 * Created by corey auger on 13/11/15.
 */
class SimpleWebRTC[M, T <: Peer.ModelTransformPeerSignaler[M]](signaler: T) extends WebRTC[M, T](signaler) {

  // TODO: pass these in...
  val rtcConfiguration = RTCConfiguration(
    iceServers = js.Array[RTCIceServer](
      RTCIceServer(url = "stun:stun.l.google.com:19302"),
      RTCIceServer(url = "turn:turn.conversant.im:443", username="turnuser", credential = "turnpass")
    )
  )
  val receiveMedia = MediaConstraints(
    mandatory = js.Dynamic.literal(OfferToReceiveAudio = true, OfferToReceiveVideo = true)
  )
  val peerConnectionConstraints = MediaConstraints(optional = js.Array[js.Dynamic](
    js.Dynamic.literal(DtlsSrtpKeyAgreement = true)
  ))

  def startLocalVideo(constraints:MediaConstraints, videoElm:dom.html.Video):Future[MediaStream] = {
    startLocalMedia(constraints).map{ stream: MediaStream =>
      (videoElm.asInstanceOf[js.Dynamic]).srcObject = stream
      stream
    }
  }

  def joinRoom(name:String):Future[Peer.Room] = {
    val p = Promise[Peer.Room]()
    signaler.send(Peer.Join(Peer.EmptyPeer, signaler.localPeer, name))
    signaler.receivers.push({
      case r:Peer.Room if r.name == name =>
        println("Got room")
        r.members.filter(_.id != signaler.localPeer.id).foreach{ m:Peer.PeerInfo =>
          val peer = createPeer( Peer.Props(
            remote = Peer.PeerInfo(m.id, m.`type`),
            local = signaler.localPeer,
            signaler = this,
            rtcConfiguration = rtcConfiguration,
            receiveMedia = receiveMedia,
            peerConnectionConstraints = peerConnectionConstraints
          ))
          peer.start
        }
        p.complete(Try(r))
      case o:Peer.Offer if o.local.id != signaler.localPeer.id =>
        println(s"GOT AN OFFER ... for ${o.local}")
        peers.find(_.remote.id == o.local.id) match{
          case Some(peer) => peer.handleMessage(o)
          case None =>
            println("Offer for new peer...")
            val peer = createPeer( Peer.Props(
              remote = o.local,
              local = signaler.localPeer,
              signaler = this,
              rtcConfiguration = rtcConfiguration,
              receiveMedia = receiveMedia,
              peerConnectionConstraints = peerConnectionConstraints
            ))
            peer.handleMessage(o)
        }

      case msg if msg.local.id != signaler.localPeer.id =>
        println(s"msg ${msg}")
        peers.foreach(_.handleMessage(msg))

      case _ =>
        println("ignore message...")
        // don't care..
    })
    p.future
  }
}
