package io.surfkit.clientlib.webrtc

import org.scalajs.dom.experimental._

/**
 * Created by corey auger on 13/11/15.
 */
class WebRTC extends LocalMedia{
  println("WebRTC")

  override def localStream(stream:MediaStream):Unit = {
    println("localStream")
  }

}
