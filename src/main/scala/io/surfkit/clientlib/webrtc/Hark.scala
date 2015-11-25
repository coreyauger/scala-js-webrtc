package io.surfkit.clientlib.webrtc

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.timers
import scala.scalajs.js.typedarray.Float32Array
import scala.util.Try
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.raw.AnalyserNode
import org.scalajs.dom
import org.scalajs.dom._
import scalajs.js


/**
 * Created by corey auger on 13/11/15.
 */

trait Hark {

  private val audioContextPromise = Promise[dom.AudioContext]
  val audioContext = audioContextPromise.future

  var speaking = false
  var interval = 100
  var threshold = 5000
  var running = false

  // events
  var onVolumeChange = (level:Double, threshold:Int) => {}
  var onSpeakingStopped = () => {}
  var onSpeaking = () => {}

  def setupAudioMonitor(stream: MediaStream, opts: Hark.Options) ={
    val ac = new dom.AudioContext()
    audioContextPromise.complete(Try(ac))
    running = true

    interval = opts.interval
    threshold = opts.threshold

    val analyser = ac.createAnalyser
    analyser.fftSize = 512
    analyser.smoothingTimeConstant = opts.smoothing
    val fftBins = new Float32Array(analyser.fftSize)

    val sourceNode = ac.createMediaStreamSource(stream)


    sourceNode.connect(analyser)
    if (opts.play) analyser.connect(ac.destination)
    speaking = false


    val speakingHistory = new js.Array[Int](opts.history)

    // Poll the analyser node to determine if speaking
    // and emit events if changed
    def looper():Unit =  {
      timers.setTimeout(interval){
        //check if stop has been called
        if(running) {
          val currentVolume = getMaxVolume(analyser, fftBins)
          onVolumeChange(currentVolume, threshold)
          if (currentVolume > threshold && !speaking) {
            // trigger quickly, short history
            val history = speakingHistory.reverse.take(4).sum
            if (history >= 2) {
              speaking = true
              onSpeaking()
            }
          } else if (currentVolume < threshold && speaking) {
            val history = speakingHistory.sum
            if (history == 0) {
              speaking = false
              onSpeakingStopped()
            }
          }
          speakingHistory.shift()
          speakingHistory.push(if(currentVolume > threshold) 1 else 0)
          looper()
        }
      }
    }
    looper
  }

  def getMaxVolume (analyser:AnalyserNode, fftBins:Float32Array):Double = {
    analyser.getFloatFrequencyData(fftBins)
    fftBins.toArray.drop(4).filter(_ != 0).max
  }

  def stopAudioMonitor = {
    running = false
    onVolumeChange(-100, threshold)
    if (speaking) {
      speaking = false
      onSpeakingStopped()
    }
  }

}

object Hark{
  case class Options(smoothing:Double = 0.1,
                     interval:Int = 50,
                     threshold:Int = -55,
                     play:Boolean = false,
                     history:Int = 10)
}
