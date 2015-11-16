package io.surfkit.clientlib.webrtc

import java.util.UUID
import org.scalajs.dom
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom.raw.{DOMError, Event}


object Peer{
  case class Props(id:String,
                   signaler: PeerSignaler,
                   rtcConfiguration:RTCConfiguration,
                   stream: MediaStream,
                   receiveMedia: MediaConstraints,
                   peerConnectionConstraints: MediaConstraints,
                   `type`:String = "video",
                   oneway:Boolean = false,
                   sharemyscreen:Boolean = false,
                   browserPrefix:Option[String] = None,
                   sid:String = UUID.randomUUID().toString
                    )

  sealed trait Signaling

  case class Offer(offer:RTCSessionDescription) extends Signaling
  case class Answer(answer:RTCSessionDescription) extends Signaling
  case class Candidate(candidate:RTCIceCandidate) extends Signaling
  case class Error(reason:String) extends Signaling

  trait PeerSignaler{
    def send(s:Signaling):Unit
  }
}

/**
 * Created by corey auger on 14/11/15.
 */
class Peer(p:Peer.Props) {

  /*this.id = options.id;
  this.type = options.type || 'video';
  this.oneway = options.oneway || false;
  this.sharemyscreen = options.sharemyscreen || false;
  this.browserPrefix = options.prefix;
  this.stream = options.stream;
  this.enableDataChannels = options.enableDataChannels === undefined ? this.parent.config.enableDataChannels : options.enableDataChannels;
  this.receiveMedia = options.receiveMedia || this.parent.config.receiveMedia;
  this.channels = {};
  this.sid = options.sid || DateTime().toString();*/
  // Create an RTCPeerConnection via the polyfill
  val pc = new RTCPeerConnection(p.rtcConfiguration, p.peerConnectionConstraints)
  val addStream = pc.addStream _
  val removeStream = pc.removeStream _

  pc.onicecandidate = { evt:RTCPeerConnectionIceEvent =>
    p.signaler.send(Peer.Candidate(evt.candidate))
  }
  pc.onnegotiationneeded = { evt:Event =>
    println("onNegotiationneeded")
  }
  pc.oniceconnectionstatechange = { evt:Event =>
    println("oniceconnectionstatechange")
    pc.iceConnectionState match {
      case IceConnectionState.failed =>
        // currently, in chrome only the initiator goes to failed
        // so we need to signal this to the peer
        if (pc.localDescription.`type` == RTCSdpType.offer) {
          println("iceFailed ")
          p.signaler.send( Peer.Error("connectivityError ICE FAILED"))
        }
    }
  }
  pc.onsignalingstatechange = { evt:Event =>
    println(s"onsignalingstatechange: ${pc.signalingState}")
  }

  def start():Unit = {
    // well, the webrtc api requires that we either
    // a) create a datachannel a priori
    // b) do a renegotiation later to add the SCTP m-line
    // Let's do (a) first...
    // TODO: ...
    //if (enableDataChannels) {
    //  getDataChannel('simplewebrtc');
    //}
    println("create offer")
    pc.createOffer({ offer:RTCSessionDescription =>
      // does not work for jingle, but jingle.js doesn't need
      // this hack...
      val expandedOffer =  RTCSessionDescription(`type` = "offer", sdp = offer.sdp)
      /*if (self.assumeSetLocalSuccess) {
        self.emit('offer', expandedOffer);
        cb(null, expandedOffer);
      }*/
      //candidateBuffer = Set.empty[RTCIceCandidate]
      println(s"offer: ${offer}")
      println("setLocalDescription")
      pc.setLocalDescription(offer,() =>{
        /*var jingle;
        if (self.config.useJingle) {
          jingle = SJJ.toSessionJSON(offer.sdp, {
            role: self._role(),
            direction: 'outgoing'
          });
          jingle.sid = self.config.sid;
          self.localDescription = jingle;

          // Save ICE credentials
          each(jingle.contents, function (content) {
            var transport = content.transport || {};
            if (transport.ufrag) {
              self.config.ice[content.name] = {
                ufrag: transport.ufrag,
                pwd: transport.pwd
              };
            }
          });

          expandedOffer.jingle = jingle;
        }*/
        //expandedOffer.sdp.split("\r\n").filter(_.startsWith("a=candidate:")).foreach(_checkLocalCandidate)
        /*if (!self.assumeSetLocalSuccess) {
          self.emit('offer', expandedOffer);
          cb(null, expandedOffer);
        }*/
        println("signal offer")
        p.signaler.send(Peer.Offer(offer))

      },handleError _)
    },handleError _,p.receiveMedia)
  }

  def handleError(err:DOMError):Unit = {
    println("ERROR")
    println(err)
  }


  def handleMessage(message:Peer.Signaling):Unit = {
    println(s"handleMessage ${message}")

    //if (message.prefix) this.browserPrefix = message.prefix;
    message match{
      case Peer.Offer(offer) =>
        pc.setRemoteDescription(offer,() => {
          println("setRemoteDescription success")
        },handleError _)
        // auto-accept
        pc.createAnswer({ answer:RTCSessionDescription =>
          println(s"createAnswer: ${answer}")
          // TODO: signal answer
          p.signaler.send(Peer.Answer(answer))
          pc.setLocalDescription(answer, () =>{
            println("setLocalDescription. success")
          },handleError _)
        },handleError _, p.receiveMedia)

      case Peer.Answer(answer) =>
        pc.setRemoteDescription(answer, () => {
          println("setRemoteDescription. success")
          // SEE   if (self.wtFirefox) { .. }  https://github.com/otalk/RTCPeerConnection/blob/master/rtcpeerconnection.js#L507
        },handleError _)

      case Peer.Candidate(candidate) =>
        pc.addIceCandidate(candidate, () => {
          println("addIceCandidate. success")
        },handleError _)

      case Peer.Error(reason) =>
        println(s"Peer sent you error: ${reason}")
    }
  }

}
