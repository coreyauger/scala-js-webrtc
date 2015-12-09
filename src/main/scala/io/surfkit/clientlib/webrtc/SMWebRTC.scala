package io.surfkit.clientlib.webrtc

import io.surfkit.clientlib.webrtc.Peer.{Signaling, PeerInfo}
import org.scalajs.dom._
import scala.concurrent.{Promise, Future}
import scala.scalajs.js
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.{JSExportAll, JSExport}
import scala.util.{Failure, Success, Try}
import org.scalajs.dom.experimental.mediastream._
import scala.scalajs.js.|

/**
 * Created by corey auger on 13/11/15.
 */

object SMWebRTC{
  sealed case class CallState(state: String)
  object CallState{
    val idle = CallState("idle")
    val call = CallState("call")
  }
}

class SMWebRTC[M, T <: Peer.ModelTransformPeerSignaler[M]](signaler: T, config: RTCConfiguration) extends WebRTC[M, T](signaler,config) {

  val blankRoom = Peer.Room(Peer.EmptyPeer, Peer.EmptyPeer, "", js.Array[Peer.PeerInfo]())
  var callState = SMWebRTC.CallState.idle
  // can only deal with a single room at a time.
  var room:Peer.Room = blankRoom

  var onJoinRoom: ((String, js.Array[Peer]) => Unit) = { (r,s) => }
  var onRing: (String, PeerInfo) => Unit = { (r,s) => }


  signaler.receivers =  js.Array[(Signaling) => Unit]()   // setup new receivers
  signaler.receivers.push({
    case r:Peer.Room =>
      println("Got room..")
      println(s"members ${room.members}")
      room = r
      room.members.filter(_.id != signaler.localPeer.id).foreach{ m:Peer.PeerInfo =>
        println("create peer")
        createPeer( Peer.Props(
          remote = Peer.PeerInfo(m.id, m.`type`),
          local = signaler.localPeer,
          signaler = signaler,
          rtcConfiguration = config
        ))
      }
      onJoinRoom(room.name, peers)    // call event after peers are added
      if(callState == SMWebRTC.CallState.call){
        getLocalStream.foreach{ s =>
          peers.foreach(_.addStream(s))  // add stream
          peers.foreach(_.start(r.name))   // start with offer..
        }
      }

    case o:Peer.Offer if o.local.id != signaler.localPeer.id =>
      println(s"GOT AN OFFER ... for ${o.local}")
      println(s"localStreams.size  ${localStreams.size }")
      callState match{
        case SMWebRTC.CallState.call if o.room == room.name  =>
          // recreate the peer and auto accepts
          removePeers(o.local.id)
          val peer = createPeer(Peer.Props(
            remote = o.local,
            local = signaler.localPeer,
            signaler = signaler,
            rtcConfiguration = config
          ))
          getLocalStream.foreach { s =>
            peer.addStream(s)
            peer.handleMessage(o)
          }

        case _  =>
          onRing(o.room, o.local)
      }

    case msg if msg.local.id != signaler.localPeer.id =>
      // TODO: should we not check the room as well ?
      println(s"msg ${msg}")
      peers.foreach(_.handleMessage(msg))

    case _ =>
      println("ignore message...") // don't care..
  })

  def getLocalStream:Future[MediaStream] = {
    val constraintTrue: Boolean | MediaTrackConstraints = true
    if (hasLocalStream)
      Future.successful(localStreams.head)
    else
      startLocalMedia(MediaStreamConstraints(constraintTrue, constraintTrue))
  }

  def joinRoom(name: String) = {
    callState = SMWebRTC.CallState.idle
    peers.foreach(_.end)
    peers = js.Array[Peer]()
    getLocalStream.foreach { stream =>
      signaler.send(Peer.Join(signaler.localPeer, signaler.localPeer, name))
    }
  }

  def retryPeer(peer:Peer) = {
    peer.iceConnectionState match{
      case IceConnectionState.connected | IceConnectionState.completed =>
        println("Already Connected.. skipping peer")
      case _ =>
        peer.start(room.name)   // try to restart the peer..
    }
  }

  def call(roomName: String) = {
    callState = SMWebRTC.CallState.call
    if(hasLocalStream && room.name == roomName){
      // we are in the room.. but trying to call.  Just retry peers that are not in a connected state
      peers.foreach(retryPeer)
    }else {
      peers.foreach(_.end)
      peers = js.Array[Peer]()
      getLocalStream.foreach { stream =>
        signaler.send(Peer.Join(signaler.localPeer, signaler.localPeer, roomName))
      }
    }
  }

  def hangup = {
    room = blankRoom
    callState = SMWebRTC.CallState.idle
  }

  def startLocalVideo(constraints:MediaStreamConstraints, videoElm:dom.html.Video):Future[MediaStream] = {
    getLocalStream.map{ stream: MediaStream =>
      val videoDyn = (videoElm.asInstanceOf[js.Dynamic])
      videoDyn.muted = true
      videoDyn.srcObject = stream
      videoDyn.style.display = "block"
      stream
    }
  }

}
