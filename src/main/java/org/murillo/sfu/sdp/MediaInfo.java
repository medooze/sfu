/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.sdp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
public class MediaInfo {
	
	public static List<String> SupportedExtensions = Arrays.asList(
			"urn:ietf:params:rtp-hdrext:ssrc-audio-level",
			"urn:ietf:params:rtp-hdrext:toffset",
			"http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time",
			"urn:3gpp:video-orientation",
			"http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01"
			//"http://www.webrtc.org/experiments/rtp-hdrext/playout-delay"
		);
	@XmlElement
	private final String type;
	@XmlElement
	private final String id;
	@XmlElement
	private DTLSInfo dtls;
	@XmlElement
	private ICEInfo ice;
	@XmlElement
	private HashMap<Integer,String> extensions = new HashMap<>();
	@XmlElement
	private HashMap<Integer,CodecInfo> codecs = new HashMap<>();
	@XmlElement
	private ArrayList<CandidateInfo> candidates = new ArrayList<>();
	
	public MediaInfo(String id, String type) {
		this.id = id;
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public String getId() {
		return id;
	}

	public DTLSInfo getDTLS() {
		return dtls;
	}

	public void setDTLS(DTLSInfo dtlsInfo) {
		this.dtls = dtlsInfo;
	}

	public ICEInfo getICE() {
		return ice;
	}

	public void setICE(ICEInfo iceInfo) {
		this.ice = iceInfo;
	}

	public void addExtension(Integer id, String name) {
		extensions.put(id, name);
	}

	public void addCodec(CodecInfo codecInfo) {
		codecs.put(codecInfo.getType(), codecInfo);
	}

	public CodecInfo getCodec(Integer type) {
		return codecs.get(type);
	}
	
	public CodecInfo getCodec(String codec) {
		for (CodecInfo info: codecs.values())
			if (info.getCodec().equalsIgnoreCase(codec))
				return info;
		return null;
	}

	public HashMap<Integer, CodecInfo> getCodecs() {
		return codecs;
	}
	
	public void addCandidate(CandidateInfo candidate) {
		candidates.add(candidate);
	}

	List<CandidateInfo> getCandidates() {
		return candidates;
	}

	public HashMap<Integer, String> getExtensions() {
		return extensions;
	}
	
	

}
