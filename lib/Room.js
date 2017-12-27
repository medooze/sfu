const EventEmitter	= require('events').EventEmitter;
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

//Get the Medooze Media Server interface
const MediaServer  = require("medooze-media-server");

const Participant = require("./Participant");

class Room
{
	constructor(id,ip)
	{
		//Store id
		this.id = id;
		
		//Create UDP server endpoint
		this.endpoint = MediaServer.createEndpoint(ip);
		
		//The comentarist set
		this.participants = new Map();
		
		//Create the room media capabilities
		this.capabilities = {
			audio : {
				codecs		: CodecInfo.MapFromNames(["opus"]),
				extensions	: new Set([
					"urn:ietf:params:rtp-hdrext:ssrc-audio-level",
					"http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01"
				])
			},
			video : {
				codecs		: CodecInfo.MapFromNames(["vp8","flexfec-03"],true),
				extensions	: new Set([
					"http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time",
					"http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01"
				])
			}
		};
		
		//No participants
		this.max = 0;
		
		//Create event emitter
		this.emitter = new EventEmitter();
		
		//Create uri
		this.uri = ["rooms",id];
		
		this.debug = function(str) {
			console.log("room["+id+"]::"+str);
		};
	}
	
	getId() 
	{
		return this.id;
	}
	
	getEndpoint() 
	{
		return this.endpoint;
	}
	
	getCapabilities() {
		return this.capabilities;
	}
	
	createParticipant(name) 
	{
		this.debug("createParticipant() "+ name);
		
		//Create participant
		const participant = new Participant(
			this.max++,
			name,
			this
		);
	
		participant.on('stream',(stream)=>{
			//Send it to the other participants
			for (let other of this.participants.values())
				//Check it is not the event source
				if (participant.getId()!=other.getId())
					//Add stream to participant
					other.addStream(stream);
		});
		
		//Wait for stopped event
		participant.on('participant::stopped', () => {
			//Delete comentarist
			this.participants.delete(participant.id);
			//emir participant change
			this.emitter.emit("participants",this.participants.values());
		});
		
		//Add to the participant to list
		this.participants.set(participant.id,participant);
		
		//emit participant change
		this.emitter.emit("participants",this.participants.values());
		
		//Done
		return participant;
	}
	
	getStreams() {
		const streams = [];
		
		//For each participant
		for (let participant of this.participants.values())
			//For each stream
			for (let stream of participant.getIncomingStreams())
				//Add participant streams
				streams.push(stream);
		//return them
		return streams;
	}
	
	getInfo() 
	{
		//Create info 
		const info = {
			id : this.id,
			participants : []
		};
		
		//For each participant
		for (let participant of this.participants.values())
			//Append it
			info.participants.push(participant.getInfo());
		
		//Return it
		return info;
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
		
	}
	
};

module.exports = Room;