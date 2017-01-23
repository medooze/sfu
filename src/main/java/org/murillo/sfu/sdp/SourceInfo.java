/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.sdp;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public  class SourceInfo {
	@XmlElement
	private final Long ssrc;
	@XmlElement
	private String cname;
	@XmlElement
	private String streamId;
	@XmlElement
	private String trakcId;

	public SourceInfo(Long ssrc) {
		this.ssrc = ssrc;
	}

	public String getCName() {
		return cname;
	}

	public void setCName(String cname) {
		this.cname = cname;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public String getTrakcId() {
		return trakcId;
	}

	public void setTrakcId(String trakcId) {
		this.trakcId = trakcId;
	}

	public Long getSSRC() {
		return ssrc;
	}
}
