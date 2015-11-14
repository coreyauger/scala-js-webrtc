package demo


import org.scalajs.dom
import org.scalajs.dom.raw.DOMError

import scala.scalajs.js
import io.surfkit.clientlib.webrtc._
import org.scalajs.dom.experimental._

object ScalaJsWebRTCExample extends js.JSApp {
  def main(): Unit = {

    val webRTC = new WebRTC()
    webRTC.start(MediaConstraints(true, true)){ stream:MediaStream =>
      println("Local stream...")
    }

  }
}
