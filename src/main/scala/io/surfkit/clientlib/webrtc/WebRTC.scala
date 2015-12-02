package io.surfkit.clientlib.webrtc

import java.util.UUID
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom._
import org.scalajs.dom.raw.{DOMError, Event}

import scala.scalajs.js

/**
 * Created by corey auger on 13/11/15.
 */
class WebRTC[M, T <: Peer.ModelTransformPeerSignaler[M]](signaler: T, config: RTCConfiguration) extends LocalMedia with Peer.PeerSignaler{
  var peers = js.Array[Peer]()

  def send(s:Peer.Signaling):Unit = signaler.send(s)

  var peerStreamAdded:(Peer) => Unit = { p => }
  var peerStreamRemoved:(Peer) => Unit = { p => }

  def createPeer(props:Peer.Props):Peer = {
    println("create peer..")
    val peer = new Peer(props)
    println("add local stream..")
    peer.onAddStream = { s:MediaStream =>
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

