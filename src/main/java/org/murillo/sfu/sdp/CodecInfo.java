/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.sdp;

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
public class CodecInfo {
	@XmlElement
	private final Integer type;
	@XmlElement
	private final String codec;
	@XmlElement(nillable = true)
	private Map<String, String> params;
	@XmlElement
	private Integer rtx;

	CodecInfo(String codec, Integer type, Map<String, String> params) {
		this.codec = codec;
		this.type = type;
		this.params = params;
	}

	public void setRTX(Integer rtx) {
		this.rtx = rtx;
	}

	public Integer getType() {
		return type;
	}

	public String getCodec() {
		return codec;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public boolean hasRtx() {
		return rtx!=null;
	}
	public Integer getRtx() {
		return rtx;
	}
	
	
}
