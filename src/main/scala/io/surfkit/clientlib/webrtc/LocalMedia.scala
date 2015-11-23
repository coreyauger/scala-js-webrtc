package io.surfkit.clientlib.webrtc

import org.scalajs.dom.raw.DOMError

import scala.concurrent.{Promise, Future}
import scala.scalajs.js
import scala.scalajs.js.Function
import org.scalajs.dom.experimental.webrtc._

import scala.util.Try


/**
 * Created by corey auger on 13/11/15.
 */

// TODO: ...
//https://github.com/otalk/hark/blob/master/hark.js#L18

trait LocalMedia {
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

  //if (!Support.support) {
  //  println("Your browser does not support local media capture.")
 // }

  def localStream(stream:MediaStream):Unit

  def localStreamStopped(stream:MediaStream):Unit

  def localScreenStopped(stream:MediaStream):Unit

  def audioOff():Unit

  def audioOn():Unit

  def videoOff():Unit

  def videoOn():Unit

  def startLocalMedia(constraints: MediaConstraints):Future[MediaStream] = {
    println("stream.. ")
    val p = Promise[MediaStream]()
    MediaDevices.getUserMedia(constraints).andThen((stream:MediaStream) => {
      println("stream.. ")
      if (constraints.audio && Config.detectSpeakingEvents) {
        // TODO:..
        //self.setupAudioMonitor(stream, self.config.harkOptions);
      }
      localStreams += stream
      if (Config.autoAdjustMic) {
        //self.gainController = new GainController(stream);
        // start out somewhat muted if we can track audio
        // TODO: ..
        //self.setMicIfEnabled(0.5);
      }
      localStream(stream)
      p.complete(Try(stream))
      //cb(stream)
    })/*.recover( (err:DOMError) => {
      println("error")
      println(err)
      if (Config.audioFallback && err.name == "DevicesNotFoundError" && constraints.video != false) {
        constraints.video = false
        start(constraints)
      }
      ""
    })*/
    p.future
  }

  def stopLocalMedia(stream:MediaStream):Unit = {
    // FIXME: duplicates cleanup code until fixed in FF
    stream.getTracks().foreach(_.stop())
    if (localStreams.contains(stream)) {
      localStreamStopped(stream)
      localStreams -= stream
    } else if(localScreens.contains(stream)) {
      localScreenStopped(stream)
      localScreens -= stream
    }
  }

  def stopStream():Unit = {
    // TODO: hark
    //if (audioMonitor) {
    //  audioMonitor.stop()
    //}
    localStreams.foreach{s =>
      s.getTracks().foreach(_.stop())
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
    stream.getTracks().foreach(_.stop())
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

  // TODO: ....
  /*
  LocalMedia.prototype.setupAudioMonitor = function (stream, harkOptions) {
    this._log('Setup audio');
    var audio = this.audioMonitor = hark(stream, harkOptions);
    var self = this;
    var timeout;

    audio.on('speaking', function () {
        self.emit('speaking');
        if (self.hardMuted) {
            return;
        }
        self.setMicIfEnabled(1);
    });

    audio.on('stopped_speaking', function () {
        if (timeout) {
            clearTimeout(timeout);
        }

        timeout = setTimeout(function () {
            self.emit('stoppedSpeaking');
            if (self.hardMuted) {
                return;
            }
            self.setMicIfEnabled(0.5);
        }, 1000);
    });
    audio.on('volume_change', function (volume, treshold) {
        self.emit('volumeChange', volume, treshold);
    });
};

// We do this as a seperate method in order to
// still leave the "setMicVolume" as a working
// method.
LocalMedia.prototype.setMicIfEnabled = function (volume) {
    if (!this.config.autoAdjustMic) {
        return;
    }
    this.gainController.setGain(volume);
};
   */
}
