/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.model;

import java.util.Map;
import org.murillo.MediaServer.Codecs;
import org.murillo.sdp.ParserException;
import org.murillo.sfu.ParticipantProxy;
import org.murillo.sfu.sdp.CandidateInfo;
import org.murillo.sfu.sdp.CodecInfo;
import org.murillo.sfu.sdp.DTLSInfo;
import org.murillo.sfu.sdp.ICEInfo;
import org.murillo.sfu.sdp.MediaInfo;
import org.murillo.sfu.sdp.SDPInfo;
import org.murillo.sfu.sdp.StreamInfo;

/**
 *
 * @author Sergio
 */
public class Participant {

	

	public interface UpdateListener {
		public void onUpdate(String update);
	}
	public interface TerminateListener {
		public void onTerminate(String reason);
	}
	
	private final String id;
	private final String name;
	private SDPInfo remote;
	private SDPInfo local;
	private StreamInfo stream;
	private Room room;
	
	private ParticipantProxy proxy;
	private TerminateListener terminateListener;
	private UpdateListener updateListener;
	
	public Participant(String id, String name, Room room) {
		//Set data
		this.id = id;
		this.name = name;
		this.room = room;
	}

	public SDPInfo processOffer(String offer) throws IllegalArgumentException, ParserException {
		//Process processOffer
		remote = SDPInfo.process(offer);
		//return it
		return remote;
	}
	
	public void setStreamInfo(StreamInfo stream) {
		this.stream = stream;
	}
	
	public SDPInfo createAnswer(DTLSInfo dtls,ICEInfo info,CandidateInfo candidate) {
		//Create local SDP info
		local = new SDPInfo();
		
		//Create audio media
		MediaInfo audio = new MediaInfo("audio", "audio");
		//Add ice and dtls info
		audio.setDTLS(dtls);
		audio.setICE(info);
		audio.addCandidate(candidate);
		//Get codec type
		CodecInfo opus = remote.getAudio().getCodec("Opus");
		//Add opus codec
		audio.addCodec(opus);
		
		//Add audio extensions
		for (Map.Entry<Integer, String> extension : remote.getAudio().getExtensions().entrySet())
			//If it is supported
			if (MediaInfo.SupportedExtensions.contains(extension.getValue()))
				//Add it
				audio.addExtension(extension.getKey(), extension.getValue());
		//Add it to answer
		local.addMedia(audio);
		
		//Create video media
		MediaInfo video = new MediaInfo("video", "video");
		//Add ice and dtls info
		video.setDTLS(dtls);
		video.setICE(info);
		video.addCandidate(candidate);
		//Get codec types
		CodecInfo vp9 = remote.getVideo().getCodec("vp9");
		CodecInfo fec = remote.getVideo().getCodec("flexfec-03");
		//Add video codecs
		video.addCodec(vp9);
		if (fec!=null)
			video.addCodec(fec);
		
		//Add video extensions
		for (Map.Entry<Integer, String> extension : remote.getVideo().getExtensions().entrySet())
			//If it is supported
			if (MediaInfo.SupportedExtensions.contains(extension.getValue()))
				//Add it
				video.addExtension(extension.getKey(), extension.getValue());
		
		//Add it to answer
		local.addMedia(video);
		
		return local;
	}

	public StreamInfo getRemoteStream() {
		return stream;
	}

	public void addLocalStream(StreamInfo stream) {
		//Add the media stream info to our local session	
		local.addStream(stream);
		//Fire update
		if (updateListener!=null)
		{
			//For some stupid reason we have to setup ACTPASS before updating
			for (MediaInfo media : local.getMedias())
				//Update it
				media.getDTLS().setSetup(Codecs.Setup.ACTPASS);
			//Call listener
			updateListener.onUpdate(local.toString());
		}
			
	}
	
	public void removeLocalStream(StreamInfo stream) {
		//Remove the media stream info to our local session	
		local.removeStream(stream);
		//Fire update
		if (updateListener!=null)
		{
			//For some stupid reason we have to setup ACTPASS before updating
			for (MediaInfo media : local.getMedias())
				//Update it
				media.getDTLS().setSetup(Codecs.Setup.ACTPASS);
			//Call listener
			updateListener.onUpdate(local.toString());
		}
	}

	public String answer() {
		return local.toString();
	}

	public void onUpdate(UpdateListener listener) {
		this.updateListener = listener;
	}
	
	public void onTerminate(TerminateListener terminateListener) {
		this.terminateListener = terminateListener;
	}

	void terminate(String reason) {
		this.terminateListener.onTerminate(reason);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setProxy(ParticipantProxy proxy) {
		this.proxy = proxy;
	}
	public ParticipantProxy getProxy() {
		return proxy;
	}

	public Room getRoom() {
		return room;
	}
	
	
}
