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
public class CreateRoomResult extends Result{
	
	public static final CommandType CMD = CommandType.CREATE_ROOM_CMD;
	
	public static final Error BadRequest (String messageId)			{ return new Error(CMD,messageId,1,"Bad request");			}
	public static final Error RoomAlreadyExits (String messageId)		{ return new Error(CMD,messageId,2,"Room already exists for that id");	}
	public static Object InternalServerError(String messageId)		{ return new Error(CMD,messageId,3,"Internal server error");		}
	
	@XmlElement
	private Room room; 

	public CreateRoomResult(String messageId,Room room) {
		super(CMD,messageId);
		this.room = room;
	}
}