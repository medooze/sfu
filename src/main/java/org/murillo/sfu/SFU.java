/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.xmlrpc.XmlRpcException;
import org.murillo.MediaServer.XmlSFUClient;
import org.murillo.sdp.ParserException;
import org.murillo.sfu.model.Room;
import org.murillo.sfu.exceptions.RoomAlreadyExitsException;
import org.murillo.sfu.exceptions.RoomNotFoundException;
import org.murillo.sfu.model.Participant;
import org.murillo.sfu.sdp.CandidateInfo;
import org.murillo.sfu.sdp.DTLSInfo;
import org.murillo.sfu.sdp.ICEInfo;
import org.murillo.sfu.sdp.MediaInfo;
import org.murillo.sfu.sdp.SDPInfo;
import org.murillo.sfu.sdp.SourceGroupInfo;
import org.murillo.sfu.sdp.StreamInfo;
import org.murillo.sfu.sdp.TrackInfo;

/**
 *
 * @author Sergio
 */
public class SFU {
	private final static ConcurrentHashMap<String,Room> rooms = new ConcurrentHashMap<>();
	
	private static MediaMixer mixer;
	
	static void init(MediaMixer mixer) 
	{
		SFU.mixer = mixer;
		//Listen for events
		mixer.setListener(new MediaMixer.Listener() {
			@Override
			public void onMediaMixerDisconnected(MediaMixer mediaMixer) {
				synchronized(rooms)
				{
					//Drop all conferences
					for (Room room : rooms.values())
						//Terminate
						room.terminate("Media mixer reconnected");
					//Clear rooms
					rooms.clear();
				}
			}
			@Override
			public void onMediaMixerReconnected(MediaMixer mediaMixer) {
				
			}
		});
		//Connect
		mixer.connect();
	}
	
	static void terminate() 
	{
		
	}
	
	static Room createRoom(String id, String title) throws RoomAlreadyExitsException, XmlRpcException
	{
		//Create new room
		Room room = new Room(id,title);
		
		//Check if it was already present
		if (rooms.putIfAbsent(id, room)!=null)
			//Error
			throw new RoomAlreadyExitsException();
		
		//Create media session
		XmlSFUClient client = mixer.createSFUClient();
		
		try {
			//Create a media session
			XmlSFUClient.RoomTransport transport = client.RoomCreate(id, mixer.getEventQueueId());
			//Set it
			room.setProxy(new RoomProxy(transport,client,mixer.getPublicIp()));
		} catch (XmlRpcException ex) {
			//Remove room
			rooms.remove(id);
			//Re-Throw error
			throw ex;
		}
		//Done
		return room;
	}

	static Participant joinRoom(String roomId, String name, String offer) throws IllegalArgumentException, ParserException, RoomNotFoundException, XmlRpcException {
		
		
		//Get room
		Room room = rooms.get(roomId);
		
		//If not foind
		if (room==null)
			//Exception
			throw  new RoomNotFoundException();
		
		//Get room proxy
		RoomProxy proxy = room.getProxy();
		
		//Create uuid for the participant
		String uuid = UUID.randomUUID().toString();
		//Create new participant
		Participant participant = new Participant(uuid,name,room);
		
		//Process offer
		SDPInfo remote = participant.processOffer(offer);
		
		//Get remote info from first media
		MediaInfo first = remote.getMedias().iterator().next();
		
		//Get DTLS and ICE info
		DTLSInfo remoteDTLS = first .getDTLS();
		ICEInfo remoteIce = first.getICE();
		
		//Create local DTLS info
		DTLSInfo localDTLS = new DTLSInfo( remoteDTLS.getSetup().reverse(), proxy.getHash(), proxy.getFingerprint());
		
		//Generate local ice credentias and candidate
		ICEInfo localICE = ICEInfo.Generate();
		CandidateInfo localCandidate = new CandidateInfo("1", 1, "UDP", 33554432-1, proxy.getIp(), proxy.getPort(), "host");
		
		//Create answer
		participant.createAnswer(localDTLS, localICE,localCandidate);
		
		HashMap<String,String> properties = new HashMap<>();
		
		//Is selfview enabled?
		if (room.getSelfviews())
			//Append property
			properties.put("selfview"	, "true");
		
		//Put data ICE data
		properties.put("ice.localUsername"	, localICE.getUfrag());
		properties.put("ice.localPassword"	, localICE.getPwd());
		properties.put("ice.remoteUsername"	, remoteIce.getUfrag());
		properties.put("ice.remotePassword"	, remoteIce.getPwd());
		//Put data DTLS data
		properties.put("dtls.setup"		, remoteDTLS.getSetup().name());
		properties.put("dtls.hash"		, remoteDTLS.getHash());
		properties.put("dtls.fingerprint"	, remoteDTLS.getFingerprint());
		
		//Get audio info
		MediaInfo audio = remote.getAudio();
		    
		//If it has audio
		if (audio!=null)
		{
			//Signal it
			properties.put("rtp.audio","true");
			//Put data RTP data
			properties.put("rtp.audio.opus.pt", audio.getCodec("opus").getType().toString());
			//Add audio extensions
			for (Map.Entry<Integer, String> extension : audio.getExtensions().entrySet())
				//Add it
				properties.put("rtp.ext."+extension.getValue()	, extension.getKey().toString());
		}
		
		//Get video info
		MediaInfo video = remote.getVideo();
		
		//If if has video
		if (video!=null)
		{
			//Signal it
			properties.put("rtp.video","true");
			//Put data RTP data
			properties.put("rtp.video.vp9.pt" , video.getCodec("vp9").getType().toString());
			properties.put("rtp.video.vp9.rtx", video.getCodec("vp9").getRtx().toString());
			//If flex fec is enabled
			if (video.hasCodec("flexfec-03")) 
				//Add RTP data for flex
				properties.put("rtp.video.flexfec.pt", video.getCodec("flexfec-03").getType().toString());
			//Add video extensions
			for (Map.Entry<Integer, String> extension : video.getExtensions().entrySet())
				//Add it
				properties.put("rtp.ext."+extension.getValue()	, extension.getKey().toString());
		}
		//Get streasm propetries
		StreamInfo remoteStream = remote.getFirstStream();
		
		//Incoming Stream data
		properties.put("remote.id", remoteStream.getId());
		
		//Get remote audio track
		TrackInfo remoteAudioTrack = remoteStream.getFirstTrack("audio");
		
		//If it is there
		if (remoteAudioTrack!=null)
			//Add ssrc
			properties.put("remote.audio.ssrc" , remoteAudioTrack.getSSRCs().get(0).toString());
		
		//Get remote audio track
		TrackInfo remoteVideoTrack = remoteStream.getFirstTrack("video");
		    
		//IF we hav it
		if (remoteVideoTrack!=null)
		{
			//If we have video track
			properties.put("remote.video.ssrc" , remoteVideoTrack.getSSRCs().get(0).toString());
			//Ensure we have RTX info
			if (remoteVideoTrack.hasSourceGroup("FID"))
				//Put RTX ssrc
				properties.put("remote.video.rtx.ssrc"	, remoteVideoTrack.getSourceGroup("FID").getSSRCs().get(1).toString());
			//Ensure we have Flex FEC info
			if (remoteVideoTrack.hasSourceGroup("FEC-FR"))
				//Put Fec ssrc
				properties.put("remote.video.fec.ssrc"	, remoteVideoTrack.getSourceGroup("FEC-FR").getSSRCs().get(1).toString());
		}
		
		//Create participant stream and sources
		StreamInfo stream = new StreamInfo(uuid);
		
		//Put stream info
		properties.put("local.id", stream.getId());
		
		//IF offer had audio
		if (audio!=null)
		{
			//Create new track for audio
			TrackInfo track = new TrackInfo("audio", uuid+"-audio");
			//Add ssrc
			track.addSSRC(room.getNextSSRC());
			//Add track
			stream.addTrack(track);
			
			//Put stream properties
			properties.put("local.audio.ssrc", stream.getFirstTrack("audio").getSSRCs().get(0).toString());
		}
		
		//IF offer had video
		if (video!=null)
		{
			//Create new track for audio
			TrackInfo track = new TrackInfo("video", uuid+"video");
			//Add ssrc
			track.addSSRC(room.getNextSSRC());

			//Get rtx group
			SourceGroupInfo fid = new SourceGroupInfo("FID", Arrays.asList(track.getSSRCs().get(0), room.getNextSSRC()));
			//Add ssrc groups
			track.addSourceGroup(fid);
			//Add ssrcs for rtx
			track.addSSRC(fid.getSSRCs().get(1));

			//Get rtx group
			SourceGroupInfo fec = new SourceGroupInfo("FEC-FR", Arrays.asList(track.getSSRCs().get(0), room.getNextSSRC()));
			//Add ssrc groups
			track.addSourceGroup(fec);
			//Add ssrcs for fec
			track.addSSRC(fec.getSSRCs().get(1));
			//Add track
			stream.addTrack(track);
			
			//Put stream properties
			properties.put("local.video.ssrc"	, stream.getFirstTrack("video").getSSRCs().get(0).toString());
			properties.put("local.video.rtx.ssrc"	, fid.getSSRCs().get(1).toString());
			properties.put("local.video.fec.ssrc"	, fec.getSSRCs().get(1).toString());
		}
		
		//Set it on participant
		participant.setStreamInfo(stream);
				
		//Crete participant on the media server
		ParticipantProxy participantProxy = proxy.addParticipant(uuid, properties);
		
		//Add it
		participant.setProxy(participantProxy);
		//Append participant
		room.addParticipant(participant);
	
		//Return it
		return participant;
	}

	static void leaveRoom(Participant participant) throws RoomNotFoundException  {

		//Get participant proxy
		ParticipantProxy proxy = participant.getProxy();
		
		try {
			//Delete participant
			proxy.destroy();
		} catch (XmlRpcException ex) {
		}
		
		 //Get room
		Room room = participant.getRoom();
		
		//If not foind
		if (room==null)
			//Exception
			throw  new RoomNotFoundException();
		
		//Remove participant
		if (room.removeParticipant(participant)==0)
		{
			//Remove room
			rooms.remove(room.getId());
			try {
				//Terminate it
				room.getProxy().destroy();
			} catch (XmlRpcException ex) {

			}
		}
		
		
	}
	
}
