package demo


import org.scalajs.dom
import org.scalajs.dom.raw.DOMError

import scala.scalajs.js
import io.surfkit.clientlib.webrtc._
import org.scalajs.dom.experimental._

object ScalaJWebRTCExample extends js.JSApp {
  def main(): Unit = {

    val webRTC = new WebRTC()
    webRTC.start(MediaConstraints(true, true)){ (err:DOMError, stream:MediaStream) =>
      println("Local stream...")
    }

  }
}
