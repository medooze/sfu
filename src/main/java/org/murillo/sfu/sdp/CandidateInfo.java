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

/**
 *
 * @author Sergio
 */
@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class CandidateInfo {
	@XmlElement
	private String fundation;
	@XmlElement
	private Integer componentId;
	@XmlElement
	private String transport;
	@XmlElement
	private Integer priority;
	@XmlElement
	private String address;
	@XmlElement
	private Integer port;
	@XmlElement
	private String type;
	@XmlElement
	private String relAddr;
	@XmlElement
	private Integer relPort;

	
	public CandidateInfo(String fundation, Integer componentId, String transport, Integer priority, String address, Integer port, String type) {
		this.fundation = fundation;
		this.componentId = componentId;
		this.transport = transport;
		this.priority = priority;
		this.address = address;
		this.port = port;
		this.type = type;
	}

	public String getFundation() {
		return fundation;
	}

	public Integer getComponentId() {
		return componentId;
	}

	public String getTransport() {
		return transport;
	}

	public Integer getPriority() {
		return priority;
	}

	public String getAddress() {
		return address;
	}

	public Integer getPort() {
		return port;
	}

	public String getType() {
		return type;
	}

	public String getRelAddr() {
		return relAddr;
	}

	public Integer getRelPort() {
		return relPort;
	}
	
}
