package io.surfkit.clientlib.webrtc

import java.util.UUID
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.raw.{DOMError, Event}

import scala.scalajs.js

/**
 * Created by corey auger on 13/11/15.
 */
class WebRTC[M, T <: Peer.ModelTransformPeerSignaler[M]](signaler: T, props:WebRTC.Props) extends LocalMedia with Peer.PeerSignaler{
  var peers = js.Array[Peer]()

  def send(s:Peer.Signaling):Unit = signaler.send(s)

  var peerStreamAdded:(Peer) => Unit = { p => }
  var peerStreamRemoved:(Peer) => Unit = { p => }

  def createPeer(props:Peer.Props):Peer = {
    val peer = new Peer(props)
    localStreams.foreach(peer.addStream(_))
    peer.onAddStream = { s:MediaStream =>
      s.oninactive = { ev:Event =>
        peerStreamRemoved(peer)
      }
      peerStreamAdded(peer)
    }
    peer.onRemoveStream = { s:MediaStream =>
      peerStreamRemoved(peer)
    }
    peers.push(peer)
    peer
  }

  def removePeers(id:String) = {
    val (rem, rest) = peers.partition(_.remote.id == id)
    rem.foreach(_.end)
    peers = rest
  }

}

object WebRTC{
  case class Props(
    rtcConfiguration:RTCConfiguration = RTCConfiguration(
      iceServers = js.Array[RTCIceServer](
        RTCIceServer(url = "stun:stun.l.google.com:19302")
      )
    ),
    receiveMedia:MediaConstraints = MediaConstraints(
      mandatory = js.Dynamic.literal(OfferToReceiveAudio = true, OfferToReceiveVideo = true)
    ),
    peerConnectionConstraints:MediaConstraints = MediaConstraints(optional = js.Array[js.Dynamic](
      js.Dynamic.literal(DtlsSrtpKeyAgreement = true)
    )))

}
