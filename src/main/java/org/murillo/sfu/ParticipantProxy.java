/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu;

import org.apache.xmlrpc.XmlRpcException;
import org.murillo.MediaServer.XmlSFUClient;

/**
 *
 * @author Sergio
 */
public class ParticipantProxy {
	private final XmlSFUClient client;
	private final Integer roomId;
	private final Integer partId;

	public ParticipantProxy(XmlSFUClient client,Integer roomId, Integer partId) {
		this.client = client;
		this.roomId = roomId;
		this.partId = partId;
	}

	public Integer getRoomId() {
		return roomId;
	}

	public Integer getPartId() {
		return partId;
	}

	void destroy() throws XmlRpcException {
		//REmove this participant from room
		client.RoomRemoveParticipant(roomId,partId);
	}

}
