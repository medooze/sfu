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
		//Get remote info
		DTLSInfo remoteDTLS = remote.getAudio().getDTLS();
		ICEInfo remoteIce = remote.getAudio().getICE();
		
		//Set dtls and ice info
		DTLSInfo localDTLS = new DTLSInfo( remoteDTLS.getSetup().reverse(), proxy.getHash(), proxy.getFingerprint());
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
		
		//Check if remote has fec
		//TODO: remove support
		boolean hasFEC = remote.getVideo().hasCodec("flexfec-03");
		
		//Put data RTP data
		properties.put("rtp.audio.opus.pt"	, remote.getAudio().getCodec("opus").getType().toString());
		properties.put("rtp.video.vp9.pt"	, remote.getVideo().getCodec("vp9").getType().toString());
		properties.put("rtp.video.vp9.rtx"	, remote.getVideo().getCodec("vp9").getRtx().toString());
		if (hasFEC) properties.put("rtp.video.flexfec.pt"	, remote.getVideo().getCodec("flexfec-03").getType().toString());
		
		//Add audio extensions
		for (Map.Entry<Integer, String> extension : remote.getAudio().getExtensions().entrySet())
			//Add it
			properties.put("rtp.ext."+extension.getValue()	, extension.getKey().toString());
		//Add video extensions
		for (Map.Entry<Integer, String> extension : remote.getVideo().getExtensions().entrySet())
			//Add it
			properties.put("rtp.ext."+extension.getValue()	, extension.getKey().toString());
		
		//Incoming Stream data
		properties.put("remote.id"		, remote.getFirstStream().getId());
		properties.put("remote.audio.ssrc"	, remote.getFirstStream().getFirstTrack("audio").getSSRCs().get(0).toString());
		properties.put("remote.video.ssrc"	, remote.getFirstStream().getFirstTrack("video").getSSRCs().get(0).toString());
		properties.put("remote.video.rtx.ssrc"	, remote.getFirstStream().getFirstTrack("video").getSourceGroup("FID").getSSRCs().get(1).toString());
		if (hasFEC) properties.put("remote.video.fec.ssrc"	, remote.getFirstStream().getFirstTrack("video").getSourceGroup("FEC-FR").getSSRCs().get(1).toString());
		
		//Create participant stream and sources
		TrackInfo track;
		StreamInfo stream = new StreamInfo(uuid);
		
		//Create new track for audio
		track = new TrackInfo("audio", uuid+"-audio");
		//Add ssrc
		track.addSSRC(room.getNextSSRC());
		//Add track
		stream.addTrack(track);
		
		//Create new track for audio
		track = new TrackInfo("video", uuid+"video");
		//Add ssrc
		track.addSSRC(room.getNextSSRC());
		
		//Get rtx group
		SourceGroupInfo fid = new SourceGroupInfo("FID"    ,Arrays.asList(track.getSSRCs().get(0),room.getNextSSRC()));
		//Add ssrc groups
		track.addSourceGroup(fid);
		//Add ssrcs for rtx
		track.addSSRC(fid.getSSRCs().get(1));
		
		if (hasFEC) 
		{
			//Get rtx group
			SourceGroupInfo fec = new SourceGroupInfo("FEC-FR" ,Arrays.asList(track.getSSRCs().get(0),room.getNextSSRC()));
			//Add ssrc groups
			track.addSourceGroup(fec);
			//Add ssrcs for fec
			track.addSSRC(fec.getSSRCs().get(1));
		}
		//Add track
		stream.addTrack(track);
		
		//Set it on participant
		participant.setStreamInfo(stream);
				
		properties.put("local.id"		, stream.getId());
		properties.put("local.audio.ssrc"	, stream.getFirstTrack("audio").getSSRCs().get(0).toString());
		properties.put("local.video.ssrc"	, stream.getFirstTrack("video").getSSRCs().get(0).toString());
		properties.put("local.video.rtx.ssrc"	, room.getNextSSRC().toString());
		if (hasFEC) properties.put("local.video.fec.ssrc"	, room.getNextSSRC().toString());
		
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
