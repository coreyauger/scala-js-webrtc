package io.surfkit.clientlib.webrtc

import java.util.UUID
import org.scalajs.dom.experimental._
import org.scalajs.dom.raw.DOMError

/**
 * Created by corey auger on 14/11/15.
 */
class Peer(p:Peer.Props) {

  /*this.id = options.id;
  this.parent = options.parent;
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
  val pc = new RTCPeerConnection(p.parent.rtcConfiguration, p.parent.peerConnectionConstraints)
  pc.onicecandidate = { evt:RTCPeerConnectionIceEvent =>
    // TODO: ..
  }
  /*pc.onoffer = { evt:RTCPeerConnectionIceEvent =>
    // TODO: ..
  }*/

  private var candidateBuffer = Set.empty[RTCIceCandidate]


  def start():Unit = {
    // well, the webrtc api requires that we either
    // a) create a datachannel a priori
    // b) do a renegotiation later to add the SCTP m-line
    // Let's do (a) first...
    // TODO: ...
    //if (enableDataChannels) {
    //  getDataChannel('simplewebrtc');
    //}

    pc.createOffer({ offer:RTCSessionDescription =>
      // does not work for jingle, but jingle.js doesn't need
      // this hack...
      // FIXME:
      //val expandedOffer =  RTCSessionDescription(`type` = "offer", sdp = offer.sdp)
      /*if (self.assumeSetLocalSuccess) {
        self.emit('offer', expandedOffer);
        cb(null, expandedOffer);
      }*/
      candidateBuffer = Set.empty[RTCIceCandidate]
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
        // FIXME: ..
       /* expandedOffer.sdp.split('\r\n').forEach(function (line) {
          if (line.indexOf('a=candidate:') === 0) {
            self._checkLocalCandidate(line);
          }
        });

        if (!self.assumeSetLocalSuccess) {
          self.emit('offer', expandedOffer);
          cb(null, expandedOffer);
        }*/
      },{ err:DOMError =>
        println("error")
        println(err)
      })
    },{ err:DOMError =>

    },p.receiveMedia)
  }
}

object Peer{
  case class Props(id:String,
                   parent:WebRTC,
                   stream: MediaStream,
                   receiveMedia: MediaConstraints,
                   `type`:String = "video",
                   oneway:Boolean = false,
                   sharemyscreen:Boolean = false,
                   browserPrefix:Option[String] = None,
                   sid:String = UUID.randomUUID().toString
                    )
}
