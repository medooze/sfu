/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.sdp;

import java.util.ArrayList;
import java.util.List;
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
public  class TrackInfo {
	@XmlElement
	private final String media;
	@XmlElement
	private final String id;
	@XmlElement
	private final ArrayList<Long> ssrcs = new ArrayList<>();
	@XmlElement
	private ArrayList<SourceGroupInfo> groups = new ArrayList<>();

	public TrackInfo(String media, String id) {
		this.media = media;
		this.id = id;
	}

	public String getMedia() {
		return media;
	}

	public String getId() {
		return id;
	}

	public void addSSRC(Long ssrc) {
		ssrcs.add(ssrc);
	}

	public List<Long> getSSRCs() {
		return ssrcs;
	}
	
	public void addSourceGroup(SourceGroupInfo group) {
		groups.add(group);
	}
	
	public SourceGroupInfo getSourceGroup(String schematics) {
		for (SourceGroupInfo group : groups)
			if (group.getSemantics().equalsIgnoreCase(schematics))
				return group;
		return null;
	}
	
	public List<SourceGroupInfo> getSourceGroups() {
		return groups;
	}

	public boolean hasSourceGroup(String schematics) {
		for (SourceGroupInfo group : groups)
			if (group.getSemantics().equalsIgnoreCase(schematics))
				return true;
		return false;
	}
}
