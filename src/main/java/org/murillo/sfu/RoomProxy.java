/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu;

import java.util.HashMap;
import org.apache.xmlrpc.XmlRpcException;
import org.murillo.MediaServer.XmlSFUClient;

/**
 *
 * @author Sergio
 */
public class RoomProxy {

	private final XmlSFUClient.RoomTransport transport;
	private final XmlSFUClient client;
	private final String ip;

	RoomProxy(XmlSFUClient.RoomTransport transport, XmlSFUClient client,String ip) {
		//Get data
		this.transport = transport;
		this.client = client;
		this.ip = ip;
	}
	
	public ParticipantProxy addParticipant(String name, HashMap<String,String> properties) throws XmlRpcException {
		//Create participant
		Integer partId = client.RoomAddParticipant(transport.getRoomId(), name, properties);
		//REturn proxy
		return new ParticipantProxy(client,transport.getRoomId(),partId);
	}
	
    
	public boolean destroy() throws XmlRpcException {
		return client.RoomDelete(transport.getRoomId());
	}

	public Integer getPort() {
		return transport.getPort();
	}

	public String getHash() {
		return transport.getHash();
	}

	public String getFingerprint() {
		return transport.getFingerprint();
	}

	public String getIp() {
		return ip;
	}
	    
	
}
