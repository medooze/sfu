/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.model;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Sergio
 */
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.murillo.sfu.RoomProxy;
import org.murillo.sfu.sdp.StreamInfo;
import org.murillo.sfu.utils.PSNRSequence;

@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class Room {
	@XmlElement
	private final String id;
	@XmlElement
	private final String title;
	
	@XmlElement
	private final ConcurrentHashMap<String,Participant> participants = new ConcurrentHashMap<>();
	
	private Boolean selfviews = false;
	
	private RoomProxy proxy;
	private final PSNRSequence ssrcs = new PSNRSequence();
	
	public Room(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public void setProxy(RoomProxy proxy) {
		this.proxy = proxy;
	}
	
	public RoomProxy getProxy() {
		return proxy;
	}
	
	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public synchronized void addParticipant(Participant participant) {
		//Get media streams from participant
		StreamInfo stream = participant.getRemoteStream();
		
		//If we have enabled selfviews
		if (selfviews)
			//Add it to self
			participant.addLocalStream(stream);
		//For each other
		for (Participant other: participants.values())
		{
			//Add their remote stream to the new participant
			participant.addLocalStream(other.getRemoteStream());
			//Add new participant stream to the already connected one
			other.addLocalStream(stream);
		}
		//Add participants
		participants.put(participant.getId(), participant);
	}
	
	public synchronized Integer removeParticipant(Participant participant) {
		//Get media streams from participant
		StreamInfo stream = participant.getRemoteStream();
		//For each other
		for (Participant other: participants.values())
			//Add new participant stream to the already connected one
			other.removeLocalStream(stream);
		//Add participants
		participants.remove(participant.getId());
		//REturn remaining ones
		return participants.size();
	}

	public ConcurrentHashMap<String, Participant> getParticipants() {
		return participants;
	}
	

	public synchronized Long getNextSSRC() {
		return (long)ssrcs.nextShort();
	}

	public void terminate(String reason) {
		//Terminate all clients
		for (Participant participant : participants.values())
			//Terminate
			participant.terminate(reason);
	}

	public Boolean getSelfviews() {
		return selfviews;
	}

	
}
