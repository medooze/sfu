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

/**
 *
 * @author Sergio
 */
@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class Error extends Result {
	@XmlElement
	Integer error;
	@XmlElement
	String message;

	public Error(CommandType cmd, String messageId, Integer error, String message) {
		super(cmd,messageId);
		this.error = error;
		this.message = message;
	}
	
	
}
