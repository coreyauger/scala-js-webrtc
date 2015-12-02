package io.surfkit.clientlib.webrtc

import org.scalajs.dom.raw.DOMError
import scala.concurrent.{Promise, Future}
import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.scalajs.js.{timers, Function}
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.experimental.mediastream._
import org.scalajs.dom._

import scala.util.Try

/**
 * Created by corey auger on 13/11/15.
 */
trait LocalMedia extends Hark{

  object Config {
    var autoAdjustMic = false
    var detectSpeakingEvents = true
    var audioFallback = false
  }

  // FIXME: ..
  //val screenSharingSupport = Support.supportScreenSharing
  var hardMuted = false
  var localStreams = Set.empty[MediaStream]
  var localScreens = Set.empty[MediaStream]
  var gainController:Option[GainController] = None

  //if (!Support.support) {
  //  println("Your browser does not support local media capture.")
 // }

  // Event handlers ...
  var localStream: (MediaStream) => Unit = { stream => println("localStream") }
  var localStreamStopped:(MediaStream) => Unit = { stream => println("localStreamStopped") }
  var localScreenStopped:(MediaStream) => Unit = { stream => println("localScreenStopped") }
  var audioOff:() => Unit = () => {}
  var audioOn:() => Unit = () => {}
  var videoOff:() => Unit = () => {}
  var videoOn:() => Unit = () => {}

  def hasLocalStream = localStreams.size > 0

  onSpeaking = () => {
    if(!hardMuted)
      gainController.foreach(_.setGain(1.0))
    println("speaking..")
  }
  var stopSpeakingTimeout:SetTimeoutHandle = timers.setTimeout(0){}
  onSpeakingStopped = () => {
    timers.clearTimeout(stopSpeakingTimeout)
    stopSpeakingTimeout = timers.setTimeout(1000){
      gainController.foreach(_.setGain(0.4))
      println("NOT speaking...")
    }
  }

  def handleError(err:DOMError):Unit = {
    println(s"[ERROR] - ${err}")
  }

  def startLocalMedia(constraints: MediaStreamConstraints):Future[MediaStream] = {
    println("stream.. ")
    val p = Promise[MediaStream]()
    // NavigatorMediaStream
    org.scalajs.dom.window.navigator.getUserMedia(constraints, { stream:MediaStream =>
      println("stream.. ")
      if (Config.detectSpeakingEvents) {
        setupAudioMonitor(stream, Hark.Options(
          play = false
        ))
      }
      localStreams += stream
      if (Config.autoAdjustMic) {
        gainController = Some(new GainController(stream))
        gainController.foreach(_.setGain(0.5))
      }
      localStream(stream)
      p.complete(Try(stream))
    }, handleError _ )
    p.future
  }

  def stopLocalMedia(stream:MediaStream):Unit = {
    // FIXME: duplicates cleanup code until fixed in FF
    stream.getAudioTracks().foreach(_.stop())
    stream.getVideoTracks().foreach(_.stop())
    if (localStreams.contains(stream)) {
      localStreamStopped(stream)
      localStreams -= stream
    } else if(localScreens.contains(stream)) {
      localScreenStopped(stream)
      localScreens -= stream
    }
  }

  def stopStream():Unit = {
    if (Config.detectSpeakingEvents) {
      stopAudioMonitor
    }
    localStreams.foreach{s =>
      s.getAudioTracks().foreach(_.stop())
      s.getVideoTracks().foreach(_.stop())
      localStreamStopped(s)
    }
    localStreams = Set.empty[MediaStream]
  }


  def startScreenShare()(cb:(MediaStream) => Unit):Unit = {
    println("startScreenShare stream..")
    // TODO: Shim this...
    /*NavigatorGetUserMedia.webkitGetScreenMedia((stream:MediaStream) => {
      localScreens += stream
      // TODO: might need to migrate to the video tracks onended
      // FIXME: firefox does not seem to trigger this...
      /*stream.onended = {
        localScreens -= stream
        localScreenStoped(stream)
      }*/
      localScreens(stream)
      cb(stream)
    },(err:DOMError) => {
      println("error")
      println(err)
    })*/
  }

  def stopScreenShare(stream:MediaStream):Unit = {
    stream.getAudioTracks().foreach(_.stop())
    stream.getVideoTracks().foreach(_.stop())
    localScreenStopped(stream)
    localScreens -= stream
  }

  def mute():Unit = {
    audioEnabled(false)
    hardMuted = true
    audioOff()
  }

  def unmute():Unit = {
    audioEnabled(true)
    hardMuted = false
    audioOn()
  }

  def pauseVideo = {
    videoEnabled(false)
    videoOff()
  }

  def resumeVideo = {
    videoEnabled(true)
    videoOn()
  }

  def pause = {
    pauseVideo
    mute
  }

  def resume = {
    resumeVideo
    unmute
  }

  private def audioEnabled(b:Boolean):Unit = {
    // work around for chrome 27 bug where disabling tracks
    // doesn't seem to work (works in canary, remove when working)
    //setMicIfEnabled(bool ? 1 : 0)
    localStreams.map(_.getAudioTracks().foreach(_.enabled = !b))
  }

  private def videoEnabled(b:Boolean):Unit = {
    // work around for chrome 27 bug where disabling tracks
    // doesn't seem to work (works in canary, remove when working)
    //setMicIfEnabled(bool ? 1 : 0)
    localStreams.map(_.getVideoTracks().foreach(_.enabled = !b))
  }

  def isAudioEnabled():Boolean = {
    localStreams.map(_.getAudioTracks().map(_.enabled).filter(_ == true)).length == localStreams.size
  }
}
