# scala-js-webrtc
ScalaJS WebRTC library.  Based loosly on SimpleWebRTC.

# Getting Started
In order to get webrtc working localy, you will at minimum need a way to signal the other peers.  For more information on webrtc read [http://www.html5rocks.com/en/tutorials/webrtc/basics/](http://www.html5rocks.com/en/tutorials/webrtc/basics/).

I provide you with a singaling server implementation in my project here:
[https://github.com/coreyauger/scala-webrtc-example](https://github.com/coreyauger/scala-webrtc-example)

Getting webrtc to work in the wild will require that you setup a TURN server.  Again more info on this is provided in the above link.

## WebRTC DOM extensions
At the time of writing this my PR has not been merged into the scala-js-dom.  In the mean time you can use my branch of the dom for testing here: [https://github.com/coreyauger/scala-js-dom/tree/webrtc-api](https://github.com/coreyauger/scala-js-dom/tree/webrtc-api)

# Usage
In most cases you will already have a server setup that uses a websocket for your application and some defined `Model`.  This implementation attempts to provide you with the glue required to translating your `Model` types to the ones required under the hood.  

There are of course other options then a websocket and so the only requirment for your singaling setup is to provide an implementation for 

```scala
trait ModelTransformPeerSignaler[T] extends PeerSignaler{
    val localPeer:PeerInfo

    var receivers = js.Array[(Signaling) => Unit]()

    def toPeerSignaling(model:T):Signaling
    def fromPeerSignaling(s:Signaling):T

    def receive(s:Signaling) = receivers.foreach(_.apply(s))
    override def send(s:Signaling):Unit = sendModel(fromPeerSignaling(s))

    // override ME
    def sendModel(model:T):Unit

  }
```

Here is a full example that uses the websocket server from my other projects code.

```scala
class WebSocketSignaler extends Peer.ModelTransformPeerSignaler[m.RTCSignal]{
  val id = (Math.random() * 1000).toInt.toString
  val `type` = "video"

  val localPeer = PeerInfo(id, `type`)

  var ws = new dom.WebSocket(s"ws://${dom.document.location.hostname}:${dom.document.location.port}/ws/${id}")
  ws.onmessage = { x: MessageEvent =>
    println(s"WS onmessage ${x.data.toString}")
    val msg = upickle.default.read[m.Model](x.data.toString)
    receive(toPeerSignaling(msg.asInstanceOf[m.RTCSignal]))
  }
  ws.onopen = { x: Event =>
    println("WS connection connected")
  }
  ws.onerror = { x: ErrorEvent =>
    println("some error has occured " + x.message)
  }
  ws.onclose = { x: CloseEvent =>
    println("WS connection CLOSED !!")
  }

  implicit def modelToPeer(p:m.Signaling.PeerInfo):Peer.PeerInfo = Peer.PeerInfo(p.id, p.`type`)
  implicit def peerToModel(p:Peer.PeerInfo):m.Signaling.PeerInfo = m.Signaling.PeerInfo(p.id, p.`type`)

  override def toPeerSignaling(model:m.RTCSignal):Peer.Signaling = model match{
    case m.Signaling.Room(r, l, name, members) =>
      import js.JSConverters._
      val peers = members.map(p => Peer.PeerInfo(id = p.id, `type` = p.`type`))
      Peer.Room(r, l, name, peers.toJSArray)
    case m.Signaling.Offer(r, l, offer) =>
      Peer.Offer(r, l, RTCSessionDescription(offer.`type`, offer.sdp))
    case m.Signaling.Candidate(r, l, c) =>
      Peer.Candidate(r, l, RTCIceCandidate(c.candidate, c.sdpMLineIndex, c.sdpMid))
    case m.Signaling.Answer(r, l, answer) =>
      Peer.Answer(r, l, RTCSessionDescription(answer.`type`, answer.sdp))
    case m.Signaling.Error(r, l, error) =>
      Peer.Error(r, l, error)
    case _ =>
      Peer.Error(Peer.PeerInfo("", ""), Peer.PeerInfo("", ""), "Unknown signaling type")
  }
  override def fromPeerSignaling(s:Peer.Signaling):m.RTCSignal = s match{
    case Peer.Join(r, l, name) =>
      m.Signaling.Join(r, l, name)
    case Peer.Room(r, l, name, members) =>
      val peers = members.map(p => m.Signaling.PeerInfo(id = p.id, `type` = p.`type`))
      m.Signaling.Room(r, l, name, peers.toSet)
    case Peer.Offer(r, l, offer) =>
      m.Signaling.Offer(r, l, m.Signaling.RTCSessionDescription(offer.`type`, offer.sdp))
    case Peer.Candidate(r, l, c) =>
      m.Signaling.Candidate(r, l, m.Signaling.RTCIceCandidate(c.candidate, c.sdpMLineIndex, c.sdpMid))
    case Peer.Answer(r, l, answer) =>
      m.Signaling.Answer(r, l, m.Signaling.RTCSessionDescription(answer.`type`, answer.sdp))
    case Peer.Error(r, l, error) =>
      m.Signaling.Error(r, l, error)
    case _ =>
      m.Signaling.Error(m.Signaling.PeerInfo("", ""), m.Signaling.PeerInfo("", ""), "Unknown signaling type")
  }

  override def sendModel(s:m.RTCSignal) = ws.send(upickle.default.write(s))
}


// ... Then you declare your WebRTC object like this.

 val signaler = new WebSocketSignaler
 val props = WebRTC.Props(
    rtcConfiguration = RTCConfiguration(
      iceServers = js.Array[RTCIceServer](RTCIceServer(url = "stun:stun.l.google.com:19302"))
    )
  )

  val webRTC = new SimpleWebRTC(signaler, props)
```

Now your only requiremnt on the server is that messages from a `local` peer get routed to their `remote` peer.
