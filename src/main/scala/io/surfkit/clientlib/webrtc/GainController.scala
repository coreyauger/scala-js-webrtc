package io.surfkit.clientlib.webrtc

import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.experimental.mediastream._
import org.scalajs.dom.raw.AnalyserNode
import org.scalajs.dom.AudioContext
import org.scalajs.dom
/**
 * Created by coreyauger on 25/11/15.
 */
class GainController(val stream:MediaStream) {

  val context =  new AudioContext
  val microphone = context.createMediaStreamSource(stream)
  val gainFilter = context.createGain()
  val destination = context.createMediaStreamDestination()
  val outputStream = destination.stream
  microphone.connect(this.gainFilter)
  gainFilter.connect(this.destination)
  stream.addTrack(outputStream.getAudioTracks()(0))
  stream.removeTrack(stream.getAudioTracks()(0))


  def setGain(gain:Double):Unit = gainFilter.gain.value = gain
  def gain():Double = gainFilter.gain.value

  def off():Unit = gainFilter.gain.value = 0
  def on():Unit = gainFilter.gain.value = 1
}
