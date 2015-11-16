package io.surfkit.clientlib.webrtc

/**
 * Crea/*ted by suroot on 14/11/15.
 */
class PeerConnection(rtcConfiguration:RTCConfiguration, peerConnectionConstraints:MediaConstraints) {
/*
// make sure this only gets enabled in Google Chrome
    // EXPERIMENTAL FLAG, might get removed without notice
    this.enableChromeNativeSimulcast = false;
    if (constraints && constraints.optional &&
            adapter.webrtcDetectedBrowser === 'chrome' &&
            navigator.appVersion.match(/Chromium\//) === null) {
        constraints.optional.forEach(function (constraint) {
            if (constraint.enableChromeNativeSimulcast) {
                self.enableChromeNativeSimulcast = true;
            }
        });
    }

    // EXPERIMENTAL FLAG, might get removed without notice
    this.enableMultiStreamHacks = false;
    if (constraints && constraints.optional &&
            adapter.webrtcDetectedBrowser === 'chrome') {
        constraints.optional.forEach(function (constraint) {
            if (constraint.enableMultiStreamHacks) {
                self.enableMultiStreamHacks = true;
            }
        });
    }
    // EXPERIMENTAL FLAG, might get removed without notice
    this.restrictBandwidth = 0;
    if (constraints && constraints.optional) {
        constraints.optional.forEach(function (constraint) {
            if (constraint.andyetRestrictBandwidth) {
                self.restrictBandwidth = constraint.andyetRestrictBandwidth;
            }
        });
    }

    // EXPERIMENTAL FLAG, might get removed without notice
    // bundle up ice candidates, only works for jingle mode
    // number > 0 is the delay to wait for additional candidates
    // ~20ms seems good
    this.batchIceCandidates = 0;
    if (constraints && constraints.optional) {
        constraints.optional.forEach(function (constraint) {
            if (constraint.andyetBatchIce) {
                self.batchIceCandidates = constraint.andyetBatchIce;
            }
        });
    }
    this.batchedIceCandidates = [];

    // EXPERIMENTAL FLAG, might get removed without notice
    // this attemps to strip out candidates with an already known foundation
    // and type -- i.e. those which are gathered via the same TURN server
    // but different transports (TURN udp, tcp and tls respectively)
    if (constraints && constraints.optional && adapter.webrtcDetectedBrowser === 'chrome') {
        constraints.optional.forEach(function (constraint) {
            if (constraint.andyetFasterICE) {
                self.eliminateDuplicateCandidates = constraint.andyetFasterICE;
            }
        });
    }
    // EXPERIMENTAL FLAG, might get removed without notice
    // when using a server such as the jitsi videobridge we don't need to signal
    // our candidates
    if (constraints && constraints.optional) {
        constraints.optional.forEach(function (constraint) {
            if (constraint.andyetDontSignalCandidates) {
                self.dontSignalCandidates = constraint.andyetDontSignalCandidates;
            }
        });
    }


    // EXPERIMENTAL FLAG, might get removed without notice
    this.assumeSetLocalSuccess = false;
    if (constraints && constraints.optional) {
        constraints.optional.forEach(function (constraint) {
            if (constraint.andyetAssumeSetLocalSuccess) {
                self.assumeSetLocalSuccess = constraint.andyetAssumeSetLocalSuccess;
            }
        });
    }

    // EXPERIMENTAL FLAG, might get removed without notice
    // working around https://bugzilla.mozilla.org/show_bug.cgi?id=1087551
    // pass in a timeout for this
    if (adapter.webrtcDetectedBrowser === 'firefox') {
        if (constraints && constraints.optional) {
            this.wtFirefox = 0;
            constraints.optional.forEach(function (constraint) {
                if (constraint.andyetFirefoxMakesMeSad) {
                    self.wtFirefox = constraint.andyetFirefoxMakesMeSad;
                    if (self.wtFirefox > 0) {
                        self.firefoxcandidatebuffer = [];
                    }
                }
            });
        }
    }
 */
  var localStream: Option[MediaStream] = None

  val pc = new RTCPeerConnection(rtcConfiguration, peerConnectionConstraints)

  val getLocalStreams = pc.getLocalStreams
  val getRemoteStreams = pc.getRemoteStreams
  val addStream = pc.addStream
  val removeStream = pc.removeStream


  // proxy some events directly
  this.pc.onremovestream = this.emit.bind(this, 'removeStream');
  this.pc.onaddstream = this.emit.bind(this, 'addStream');
  this.pc.onnegotiationneeded = this.emit.bind(this, 'negotiationNeeded');
  this.pc.oniceconnectionstatechange = this.emit.bind(this, 'iceConnectionStateChange');
  this.pc.onsignalingstatechange = this.emit.bind(this, 'signalingStateChange');

  // handle ice candidate and data channel events
  this.pc.onicecandidate = this._onIce.bind(this);
  this.pc.ondatachannel = this._onDataChannel.bind(this);

  this.localDescription = {
    contents: []
  };
  this.remoteDescription = {
    contents: []
  };

  this.config = {
    debug: false,
    ice: {},
    sid: '',
    isInitiator: true,
    sdpSessionID: Date.now(),
    useJingle: false
  };

  // apply our config
  /*for (item in config) {
    this.config[item] = config[item];
  }

  if (this.config.debug) {
    this.on('*', function () {
      var logger = config.logger || console;
      logger.log('PeerConnection event:', arguments);
    });
  }*/
  var hadLocalStunCandidate = false
  var hadRemoteStunCandidate = false
  var hadLocalRelayCandidate = false
  var hadRemoteRelayCandidate = false

  var hadLocalIPv6Candidate = false
  var hadRemoteIPv6Candidate = false

  // keeping references for all our data channels
  // so they dont get garbage collected
  // can be removed once the following bugs have been fixed
  // https://crbug.com/405545
  // https://bugzilla.mozilla.org/show_bug.cgi?id=964092
  // to be filed for opera
  var _remoteDataChannels = Nil
  var _localDataChannels = Nil

  var _candidateBuffer = Nil

  val signalingState = pc.signalingState
  val iceConnectionState = pc.iceConnectionState

  private def role = if(isInitiator)"initiator" else "responder"

  def addStream(stream:MediaStream):Unit = {
    localStream = Some(stream)
    pc.addStream(stream)
  }

  private def checkLocalCandidate(candidate:RTCIceCandidate) = {
    var cand = SJJ.toCandidateJSON(candidate)
    if (cand.type == 'srflx') {
      this.hadLocalStunCandidate = true;
    } else if (cand.type == 'relay') {
      this.hadLocalRelayCandidate = true;
    }
    if (cand.ip.indexOf(':') != -1) {
      this.hadLocalIPv6Candidate = true;
    }
  }
}
*/