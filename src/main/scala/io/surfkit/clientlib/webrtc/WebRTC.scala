package io.surfkit.clientlib.webrtc

import java.util.UUID
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.experimental.mediastream._
import org.scalajs.dom.raw.{DOMError, Event}
import org.scalajs.dom

import scala.scalajs.js

/**
 * Created by corey auger on 13/11/15.
 */
class WebRTC[M, T <: Peer.ModelTransformPeerSignaler[M]](val signaler: T, config: RTCConfiguration) extends LocalMedia with Peer.PeerSignaler{
  var peers = js.Array[Peer]()

  def send(s:Peer.Signaling):Unit = signaler.send(s)

  var peerStreamAdded:(Peer) => Unit = { p => }
  var peerStreamRemoved:(Peer) => Unit = { p => }
  var peerIceConnectionStateChange:(Peer) => Unit = { p => }
  var peerSignalingStateChange:(Peer) => Unit = { p => }
  var peerCreated:(Peer) => Unit = { p => }

  def createPeer(props:Peer.Props):Peer = {
    println("create peer..")
    val peer = new Peer(props)
    println("add local stream..")
    peer.onAddStream = { s:MediaStream =>
      println("Got a MediaStream calling peerStreamAdded")
      peerStreamAdded(peer)
      dom.window.console.log(peerStreamAdded.asInstanceOf[js.Dynamic])
    }
    peer.onRemoveStream = { s:MediaStream =>
      peerStreamRemoved(peer)
    }
    peer.onSignalingStateChange = { s =>
      peerSignalingStateChange(peer)
    }
    peer.onIceConnectionStateChange = { s =>
      peerIceConnectionStateChange(peer)
    }
    peers.push(peer)
    peerCreated(peer)
    peer
  }

  def removePeers(id:String) = {
    val (rem, rest) = peers.partition(_.remote.id == id)
    rem.foreach(_.end)
    peers = rest
  }

}

