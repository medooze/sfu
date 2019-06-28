const EventEmitter	= require('events').EventEmitter;
const Logger		= require("./Logger");
//Get Semantic SDP objects
const SemanticSDP	= require("semantic-sdp");
const SDPInfo		= SemanticSDP.SDPInfo;
const MediaInfo		= SemanticSDP.MediaInfo;
const CandidateInfo	= SemanticSDP.CandidateInfo;
const DTLSInfo		= SemanticSDP.DTLSInfo;
const ICEInfo		= SemanticSDP.ICEInfo;
const StreamInfo	= SemanticSDP.StreamInfo;
const TrackInfo		= SemanticSDP.TrackInfo;
const Direction		= SemanticSDP.Direction;
const CodecInfo		= SemanticSDP.CodecInfo;


class Participant
{
	constructor(id,name,room)
	{
		//Store props
		this.id = id;
		this.name = name;
		
		//And casting
		this.room = room;
		
		//Create event emitter
		this.emitter = new EventEmitter();
		
		//Streams
		this.incomingStreams = new Map();
		this.outgoingStreams = new Map();
		
		//SDP info
		this.localSDP = null;
		this.remoteSDP = null;
		
		//Create uri
		this.uri = room.uri.concat(["participants",id]);
		
		//Get child logger
		this.logger = room.logger.child("participants["+id+"]");
	}
	
	getId() 
	{
		return this.id;
	}
	
	init(sdp) {
		//Log
		this.logger.info("init");
		
		//Get data
		const endpoint  = this.room.getEndpoint();
		
		//Create an DTLS ICE transport in that enpoint
		this.transport = endpoint.createTransport({
			dtls : sdp.getDTLS(),
			ice  : sdp.getICE() 
		});
		
		//Enable bandwidth probing
		this.transport.setBandwidthProbing(true);
		this.transport.setMaxProbingBitrate(256000);
	
		//Dump contents
		//this.transport.dump("/tmp/sfu-"+this.uri.join("-")+".pcap");

		//Set RTP remote properties
		this.transport.setRemoteProperties({
			audio : sdp.getMedia("audio"),
			video : sdp.getMedia("video")
		});

		//Create local SDP info
		const answer = sdp.answer({
			dtls		: this.transport.getLocalDTLSInfo(),
			ice		: this.transport.getLocalICEInfo(),
			candidates	: endpoint.getLocalCandidates(),
			capabilities	: this.room.getCapabilities()
		});
		
		//Set RTP local  properties
		this.transport.setLocalProperties({
			audio : answer.getMedia("audio"),
			video : answer.getMedia("video")
		});
		
		//All good
		this.localSDP = answer;
		this.remoteSDP = sdp;
		
		//Bitrate estimator
		this.transport.on("targetbitrate",(targetbitrate)=>{
			//If no other participants
			if (this.outgoingStreams.length)
				//Done
				return;
			
			//Split bitrate evenly
			let bitrate = targetbitrate/this.outgoingStreams.length;
			let assigned = 0;
			//For each stream
			for (const [streamId,stream] of this.outgoingStreams)
			{
				//Get video track
				const videoTrack = stream.getVideoTracks()[0];
				//Get transponder
				const transponder = videoTrack.getTransponder();
				//Set it
				assigned += transponder.setTargetBitrate(bitrate);
			}
			
		});
	}
		
	publishStream(streamInfo)
	{
		this.logger.info("publishStream()");
		
		//If already publishing
		if (!this.transport)
			throw Error("Not inited");

		//Create the remote participant stream into the transport
		const incomingStream = this.transport.createIncomingStream(streamInfo);
		
		//Add origin
		incomingStream.uri = this.uri.concat(["incomingStreams",incomingStream.getId()]);

		//Append
		this.incomingStreams.set(incomingStream.id,incomingStream);

		//Publish stream
		this.logger.info("onstream");
		this.emitter.emit("stream",incomingStream);
	}
	
	addStream(stream) {
		
		this.logger.info("addStream() "+stream.uri.join("/"));
		
		//Create sfu local stream
		const outgoingStream = this.transport.createOutgoingStream({
			audio: true,
			video: true
		});
		
		//Add uri
		outgoingStream.uri = this.uri.concat(["outgoingStreams",outgoingStream.getId()]);
		
		//Get local stream info
		const info = outgoingStream.getStreamInfo();
		
		//Add to local SDP
		this.localSDP.addStream(outgoingStream.getStreamInfo());
		
		//Append
		this.outgoingStreams.set(outgoingStream.getId(),outgoingStream);
			
		//Emit event
		this.logger.info("onrenegotiationneeded");
		this.emitter.emit("renegotiationneeded", this.localSDP);
		
		//Attach
		outgoingStream.attachTo(stream);
		
		//Listen when this stream is removed & stopped
		stream.on("stopped",()=>{
			//If we are already stopped
			if (!this.outgoingStreams)
				//Do nothing
				return;
			//Remove stream from outgoing streams
			this.outgoingStreams.delete(outgoingStream.getId());
			//Remove from sdp
			this.localSDP.removeStream(info);
			//Emit event
			this.logger.info("onrenegotiationneeded");
			this.emitter.emit("renegotiationneeded", this.localSDP);
			
			//Remove stream
			outgoingStream.stop();
		});
	}
	
		
	getInfo() {
		//Create info 
		const info = {
			id	: this.id,
			name	: this.name,
			streams : [
				this.incomingStream ? this.incomingStream.getId() : undefined
			]
		};
		
		//Return it
		return info;
	}
	
	getLocalSDP() {
		return this.localSDP;
	}
	
	getRemoteSDO() {
		return this.remoteSDP;
	}
	
	getIncomingStreams() {
		return this.incomingStreams.values();
	}
	
	/**
	 * Add event listener
	 * @param {String} event	- Event name 
	 * @param {function} listeener	- Event listener
	 * @returns {Transport} 
	 */
	on() 
	{
		//Delegate event listeners to event emitter
		this.emitter.on.apply(this.emitter, arguments);  
		//Return object so it can be chained
		return this;
	}
	
	/**
	 * Remove event listener
	 * @param {String} event	- Event name 
	 * @param {function} listener	- Event listener
	 * @returns {Transport} 
	 */
	off() 
	{
		//Delegate event listeners to event emitter
		this.emitter.removeListener.apply(this.emitter, arguments);
		//Return object so it can be chained
		return this;
	}
	
	stop() 
	{
		this.logger.info("stop");
		
		//remove all published streams
		for (let stream of this.incomingStreams.values())
			//Stop it
			stream.stop();
		

		//Remove all emitting streams
		for (let stream of this.outgoingStreams.values())
			//Stop it
			stream.stop();
			
		//IF we hve a transport
		if (this.transport)
			//Stop transport
			this.transport.stop();
		
		//Clean them
		this.room = null;
		this.incomingStreams = null;
		this.outgoingStreams = null;
		this.transport = null;
		this.localSDP = null;
		this.remoteSDP = null;
	
		//Done
		this.logger.info("onstopped");
		this.emitter.emit("stopped");
	}
};

module.exports = Participant;
