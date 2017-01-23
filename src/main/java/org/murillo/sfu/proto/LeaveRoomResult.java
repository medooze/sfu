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
import org.murillo.sfu.model.Room;

/**
 *
 * @author Sergio
 */
@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class LeaveRoomResult extends Result{
	
	public static final CommandType CMD = CommandType.LEAVE_ROOM_CMD;
	
	public static final Error BadRequest (String messageId)			{ return new Error(CMD,messageId,1,"Bad request");			}
	public LeaveRoomResult(String messageId) {
		super(CMD,messageId);
	}
}