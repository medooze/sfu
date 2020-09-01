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


class Publication
{
	constructor(id,room)
	{
		//Store props
		this.id = id;
		
		//And casting
		this.room = room;
		
		//Create event emitter
		this.emitter = new EventEmitter();
		
		//Streams
		this.incomingStreams = new Map();
		
		//SDP info
		this.localSDP = null;
		this.remoteSDP = null;
		
		//Create uri
		this.uri = room.uri.concat(["publications",id]);
		
		//Get child logger
		this.logger = room.logger.child("publications["+id+"]");
	}
	
	getId() 
	{
		return this.id;
	}
	
	init(offer) {
		//Log
		this.logger.info("init");
		
		//Get data
		const endpoint  = this.room.getEndpoint();
		
		//Create an DTLS ICE transport in that enpoint
		this.transport = endpoint.createTransport({
			dtls : offer.getDTLS(),
			ice  : offer.getICE() 
		});
		
		//Listen for publish disconnection
		this.transport.on("icetimeout" ,()=>{
			//Log
			this.logger.error("ICE timeout");
			//Stopt publication
			this.stop();
		});
		this.transport.on("dtlsstate" ,(state)=>{
			//Log
			this.logger.info("DTLS state " + state)
			//Check if it was a gracegull disconnection or a failure
			if (state=="failed" || state=="closed")
				//Stopt publication
				this.stop();
		});
		
		//Set RTP remote properties
		this.transport.setRemoteProperties({
			audio : offer.getMedia("audio"),
			video : offer.getMedia("video")
		});

		//Create local SDP info
		const answer = offer.answer({
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
		this.remoteSDP = offer;
		
		//For all remote streams
		for (let streamInfo of this.remoteSDP.getStreams().values())
		{
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

		return answer;
	}
	
		
	getInfo()
	{
		//Create info 
		const info = {
			id	: this.id,
			name	: this.name
		};
		
		//Return it
		return info;
	}
	
	getLocalSDP() {
		return this.localSDP;
	}
	
	getRemoteSDP() {
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
		
		//IF we hve a transport
		if (this.transport)
			//Stop transport
			this.transport.stop();
		
		//Clean them
		this.room = null;
		this.incomingStreams = null;
		this.transport = null;
		this.localSDP = null;
		this.remoteSDP = null;
	
		//Done
		this.logger.info("onstopped");
		this.emitter.emit("stopped");
	}
};

module.exports = Publication;
