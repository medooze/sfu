/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.proto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class Indication {
	@XmlElement
	private CommandType cmd;

	public Indication(CommandType cmd) {
		this.cmd = cmd;
	}
}
