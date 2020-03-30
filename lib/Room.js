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
				codecs		: ["opus"],
				extensions	: ["urn:ietf:params:rtp-hdrext:ssrc-audio-level"]
			},
			video : {
				codecs		: ["vp9"],
				rtx		: true,
				rtcpfbs		:  [
					{ "id": "transport-cc"},
					{ "id": "ccm", "params": ["fir"]},
					{ "id": "nack"},
					{ "id": "nack", "params": ["pli"]},
				],
				extensions	: [ "http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01", "urn:3gpp:video-orientation"]
			}
		};
		
		//No participants
		this.max = 0;
		
		//Create event emitter
		this.emitter = new EventEmitter();
		
		//Active speaker detection
		this.activeSpeakerDetector = MediaServer.createActiveSpeakerDetector();
		
		//When new speaker detected
		this.activeSpeakerDetector.on("activespeakerchanged",(track)=>{
			//Get active speaker id
			const speakerId = track.participant.getId();
			//Check if it is the same as current one
			if (this.speakerId===speakerId)
				//Do nothing
				return;
			//Update speaker
			this.speakerId = speakerId;
			
			//Log
			this.logger.debug("activespeakerchanged speakerId=%d",speakerId);
			
			//Relaunch event
			this.emitter.emit("activespeakerchanged",track.participant,track);
		});
		//Create uri
		this.uri = ["rooms",id];
		
		//Create logger
		this.logger = new Logger("rooms["+this.id+"]");
		
		//Log
		this.logger.info("created()");
	}
	
	getId() 
	{
		return this.id;
	}
	
	getSpeakerId()
	{
		return this.speakerId;
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
		this.logger.info("createParticipant() "+ name);
		
		//Create participant
		const participant = new Participant(
			this.max++,
			name,
			this.endpoint.createSDPManager("unified-plan",this.capabilities),
			this
		);
	
		//Listener for any new publisher stream
		participant.on('stream',(stream)=>{
			//Send it to the other participants
			for (let other of this.participants.values())
				//Check it is not the event source
				if (participant.getId()!=other.getId())
					//Add stream to participant
					other.addStream(stream);
			//Get audio tracks
			const audioTracks = stream.getAudioTracks();
			//If there are any
			if (audioTracks.length)
			{
				//Get participant id
				const participantId = participant.getId();
				//And firsst audio track
				const audioTrack = audioTracks[0];
				//Only log for now
				this.logger.debug("addSpeaker() [participant:%s]",participantId);
				//Store participant
				audioTrack.participant = participant;
				audioTrack.stream	   = stream;
				//Add to detecotr
				this.activeSpeakerDetector.addSpeaker(audioTrack);
			}
			
			//Get video tracks
			const videoTracks = stream.getVideoTracks();
			//For each video track
			for (let i=0; i<videoTracks.length; ++i)
				//Request an iframe so playback can start even if no other participant has subscribed
				videoTracks[i].refresh();
		});
		
		//Wait for stopped event
		participant.on('stopped', () => {
			//Delete comentarist
			this.participants.delete(participant.id);
			//emir participant change
			this.emitter.emit("participants",this.participants.values());
		});
		
		//Add to the participant to list
		this.participants.set(participant.id,participant);
		
		//emit participant change
		this.emitter.emit("participants",this.getInfo().participants);
		
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
		if (!this.endpoint)
			return;
		
		//Log
		this.logger.info("stop()");
		
		//Stop vad
		this.activeSpeakerDetector.stop();
		
		//For each participant
		for (const [id,participant] of this.participants)
			//Stop it with reason
			participant.stop("destroyed");
		
		//Stop endpoint
		this.endpoint.stop();
		
		//Emit event
		this.logger.info("stopped");
		this.emitter.emit("stopped");
		
		//Null things
		this.activeSpeakerDetector = null;
		this.participants = null;
		this.endpoint = null;
		this.emitter = null;
	}
	
};

module.exports = Room;
