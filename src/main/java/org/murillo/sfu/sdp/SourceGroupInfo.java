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
public class SourceGroupInfo {
	
	@XmlElement
	private String semantics;
	@XmlElement
	private ArrayList<Long> ssrcs = new ArrayList<>();
	
	public SourceGroupInfo(String semantics, List<Long> ssrcs) {
		this.semantics = semantics;
		this.ssrcs.addAll(ssrcs);
	}

	public String getSemantics() {
		return semantics;
	}

	public ArrayList<Long> getSSRCs() {
		return ssrcs;
	}
	
	
}
