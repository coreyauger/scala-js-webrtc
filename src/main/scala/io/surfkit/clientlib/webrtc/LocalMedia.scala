package io.surfkit.clientlib.webrtc

import org.scalajs.dom.raw.DOMError

import scala.scalajs.js
import org.scalajs.dom.experimental._


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

  var localStreams = Set.empty[MediaStream]
  var localScreens = Set.empty[MediaStream]

  //if (!Support.support) {
  //  println("Your browser does not support local media capture.")
 // }

  def localStream(stream:MediaStream):Unit

  def start(constraints: MediaConstraints)(cb:(DOMError, MediaStream) => Unit):Unit = {
    // TODO: Shim this...
    val getUserMedia = Support.getUserMedia.asInstanceOf[js.Function2[Any, Any, Any]]
    getUserMedia(constraints, (err:DOMError, stream:MediaStream) => {
      if (js.isUndefined(err)) {
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

        // TODO: might need to migrate to the video tracks onended
        // FIXME: firefox does not seem to trigger this...
        //stream.onended = function () {
          /*
          var idx = self.localStreams.indexOf(stream);
          if (idx > -1) {
              self.localScreens.splice(idx, 1);
          }
          self.emit('localStreamStopped', stream);
          */
        //}

        localStream(stream)
      } else {
        // Fallback for users without a camera
        if (Config.audioFallback && err.name == "DevicesNotFoundError" && constraints.video != false) {
          constraints.video = false
          start(constraints)(cb)
        }
      }
      cb(err, stream)
    })
  }
}
