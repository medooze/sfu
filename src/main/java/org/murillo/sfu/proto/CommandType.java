/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.proto;

/**
 *
 * @author Sergio
 */
public enum CommandType {
	CREATE_ROOM_CMD,
	JOIN_ROOM_CMD,
	PARTICIPANT_UPDATE_IND,
	PARTICIPANT_TERMINATION_IND,
	LEAVE_ROOM_CMD,
	TERMINATE_ROOM_CMD;

	public boolean is(String command) {
		return toString().equalsIgnoreCase(command);
	}
};
