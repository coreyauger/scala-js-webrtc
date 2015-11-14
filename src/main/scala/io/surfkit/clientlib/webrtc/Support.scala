package io.surfkit.clientlib.webrtc

import org.scalajs.dom
import org.scalajs.dom.html
import scala.util.Try
import scalajs.js

/**
  * Created by corey auger on 13/11/15.
  */

object Support {
  
  val win = dom.window.asInstanceOf[js.Dynamic]
  val nav = dom.window.navigator.asInstanceOf[js.Dynamic]
  
  val ffRe = """/Firefox\/([0-9]+)\./""".r
  val wkRe = """/Chrom(e|ium)\/([0-9]+)\./"""r
   val (prefix, version) =
     if( js.isUndefined(win.mozRTCPeerConnection) || !js.isUndefined(nav.mozGetUserMedia) )
       ("moz", Try(ffRe.findAllIn(dom.window.navigator.userAgent).group(1).toInt).toOption.getOrElse[Int](0))
     else if( js.isUndefined(win.webkitRTCPeerConnection) || !js.isUndefined(nav.webkitGetUserMedia) )
       ("webkit", Try(wkRe.findAllIn(dom.window.navigator.userAgent).group(2).toInt).toOption.getOrElse[Int](0))
     else
       ("",0)

  val RTCPeerConnection =
    if( !js.isUndefined(win.RTCPeerConnection))win.RTCPeerConnection
    else if( !js.isUndefined(win.mozRTCPeerConnection))win.mozRTCPeerConnection
    else if( !js.isUndefined(win.webkitRTCPeerConnection))win.webkitRTCPeerConnection
    else js.undefined

  val IceCandidate =
    if( !js.isUndefined(win.mozRTCIceCandidate))win.mozRTCIceCandidate
    else if( !js.isUndefined(win.RTCIceCandidate))win.RTCIceCandidate
    else js.undefined

  val SessionDescription =
    if( !js.isUndefined(win.mozRTCSessionDescription))win.mozRTCSessionDescription
    else if( !js.isUndefined(win.RTCSessionDescription))win.RTCSessionDescription
    else js.undefined

  val MediaStream =
    if( !js.isUndefined(win.webkitMediaStream))win.webkitMediaStream
    else if( !js.isUndefined(win.MediaStream))win.MediaStream
    else js.undefined

  val screenSharing = (//dom.window.location.protocol == "https:" &&
      ((prefix == "webkit" && version >= 26) || (prefix == "moz" && version >= 33)))

  //val AudioContext = dom.AudioContext

 // val videoEl = dom.document.createElement("video").asInstanceOf[org.scalajs.dom.html.Video]

  //val supportVp8 = videoEl.canPlayType("video/webm; codecs=\"vp8\", vorbis") == "probably"

  val getUserMedia =
    if( !js.isUndefined(nav.getUserMedia))nav.getUserMedia
    else if( !js.isUndefined(nav.webkitGetUserMedia))nav.webkitGetUserMedia
    else if( !js.isUndefined(nav.msGetUserMedia))nav.msGetUserMedia
    else if( !js.isUndefined(nav.mozGetUserMedia))nav.mozGetUserMedia
    else js.undefined

  val supportRTCPeerConnection = !js.isUndefined(RTCPeerConnection)
  val supportGetUserMedia = !js.isUndefined(getUserMedia)
  val support = supportRTCPeerConnection && supportGetUserMedia

  //val supportDataChannel = supportRTCPeerConnection && !js.isUndefined(RTCPeerConnection.prototype) && !js.isUndefined(RTCPeerConnection.createDataChannel)
  //val supportWebAudio = !js.isUndefined(AudioContext) && !js.isUndefined(AudioContext.prototype.createMediaStreamSource)
  //val supportMediaStream = !js.isUndefined(MediaStream) && !js.isUndefined(MediaStream.prototype.removeTrack)
  val supportScreenSharing = !js.isUndefined(screenSharing)

}
