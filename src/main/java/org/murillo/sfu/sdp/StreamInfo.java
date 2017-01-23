/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.sdp;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author Sergio
 */
@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class StreamInfo {
	@XmlElement
	private final String id;
	@XmlElement
	private final HashMap<String,TrackInfo> tracks = new HashMap<>();

	public StreamInfo(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public void addTrack(TrackInfo track) {
		tracks.put(track.getId(),track);
	}

	public TrackInfo getFirstTrack(String media) {
		for(TrackInfo track : tracks.values())
			if (track.getMedia().equalsIgnoreCase(media))
				return track;
		return null;
	}

	public Map<String,TrackInfo> getTracks() {
		return tracks;
	}

	public void removeAllTracks() {
		tracks.clear();
	}

	public TrackInfo getTrack(String trackId) {
		return tracks.get(trackId);
	}
}
