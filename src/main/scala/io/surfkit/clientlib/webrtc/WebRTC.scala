package io.surfkit.clientlib.webrtc

import org.scalajs.dom.experimental._


import scala.scalajs.js

/**
 * Created by corey auger on 13/11/15.
 */
class WebRTC extends LocalMedia{
  println("WebRTC")

  val rtcConfiguration = RTCConfiguration(
    iceServers = js.Array[RTCIceServer](
      RTCIceServer(username = "stun:stun.l.google.com:19302")
    )
  )

  val receiveMedia = MediaConstraints(
    mandatory = js.Dynamic.literal(OfferToReceiveAudio = true, OfferToReceiveVideo = true)
  )
/*
  val peer = new Peer(Peer.Props(

  ))*/

  val peerConnectionConstraints = MediaConstraints(optional = js.Array[js.Dynamic](
    js.Dynamic.literal(DtlsSrtpKeyAgreement = true)
  ))

  override def localStream(stream:MediaStream):Unit = {
    println("localStream")
  }
  override def localStreamStopped(stream:MediaStream):Unit = {
    println("localStreamStopped")
  }
  override def localScreenStopped(stream:MediaStream):Unit = {
    println("localScreenStopped")
  }

  override def audioOff():Unit = {
    println("Audio Off")
  }

  override def audioOn():Unit = {
    println("Audio On")
  }

  override def videoOff():Unit = {
    println("video Off")
  }

  override def videoOn():Unit = {
    println("video On")
  }

}
