/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu;

import org.murillo.sfu.exceptions.RoomAlreadyExitsException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.murillo.sdp.ParserException;
import org.murillo.sfu.exceptions.RoomNotFoundException;
import org.murillo.sfu.model.Participant;
import org.murillo.sfu.model.Room;
import org.murillo.sfu.proto.CommandType;
import org.murillo.sfu.proto.CreateRoomResult;
import org.murillo.sfu.proto.JoinRoomResult;
import org.murillo.sfu.proto.LeaveRoomResult;
import org.murillo.sfu.proto.ParticipantTerminationIndication;
import org.murillo.sfu.proto.ParticipantUpdateIndication;
import org.murillo.sfu.utils.JSON;

/**
 *
 * @author Sergio
 */
public class Session {

	private final Long id;
	private final ChannelHandlerContext context;
	private static final Logger logger = Logger.getLogger(Session.class);
	private Participant participant;

	public Session(Long id, ChannelHandlerContext context) {
		this.id = id;
		this.context = context;
	}

	public Long getId() {
		return id;
	}

	public ChannelHandlerContext getContext() {
		return context;
	}
	
	public void send(Object msg) {

		try {
			//Convert to JSON
			String json = JSON.Stringify(msg);
			//Write
			context.writeAndFlush( new TextWebSocketFrame( json ) );
		} catch (Exception ex) {
			logger.warn("Error sending json",ex);
		}
	}

	void onMessage(JSONObject json) throws JSONException {
		
		//Get command
		String command = json.optString("cmd",null);
		//Check command
		if (command==null)
			//Ingore results by now
			return;

		//Get message id
		String messageId = json.getString("messageId");

		logger.info("Websocket cmd " + command);
		
		//TODO: REMOVE!!!
		if ("SELECT_LAYER".equalsIgnoreCase(command))
		{
			//Get attributes
			Integer spatialLayerId = json.getInt("spatialLayerId");
			Integer temporalLayerId = json.getInt("temporalLayerId");
			participant.getProxy().SelectLayer(spatialLayerId, temporalLayerId);
			
		}
		// Check commands
		else if (CommandType.CREATE_ROOM_CMD.is(command) )
		{
			
			try {
				//Get attributes
				String roomId = json.getString("id");
				String title = json.optString("title",roomId);
			
				//Create client
				Room room = SFU.createRoom(roomId,title);
				
				//Set self views
				room.setSelfViews(json.optBoolean("selfviews",false));

				//Send back result
				send(new CreateRoomResult(messageId,room));
			} catch (JSONException ex) {
				//Error
				send(CreateRoomResult.BadRequest(messageId));
			} catch (RoomAlreadyExitsException ex2) {
				//Error
				send(CreateRoomResult.RoomAlreadyExits(messageId));
			} catch (XmlRpcException ex3) {
				//Log
				logger.error("XMLRPC error",ex3);
				//Error
				send(CreateRoomResult.InternalServerError(messageId));
			}
		}
		else if (CommandType.JOIN_ROOM_CMD.is(command) )
		{
			try {
				//Get attributes
				String roomId = json.getString("roomId");
				String name = json.getString("name");
				String offer = json.getString("offer");
				
				//Create client
				participant = SFU.joinRoom(roomId,name,offer);
				
				//We want to listen even updates
				participant.onUpdate(new Participant.UpdateListener() {
					@Override
					public void onUpdate(String update) {
						send(new ParticipantUpdateIndication(update));
					}
				});
				//Listen for terminate events
				participant.onTerminate(new Participant.TerminateListener() {
					@Override
					public void onTerminate(String reason) {
						send(new ParticipantTerminationIndication(reason));
					}
				});

				//Send back result
				send(new JoinRoomResult(messageId,participant.answer()));
			} catch (JSONException ex) {
				//Error
				send(JoinRoomResult.BadRequest(messageId));
			} catch (RoomNotFoundException ex2) {
				//Error
				send(JoinRoomResult.RoomNotFound(messageId));
			} catch (XmlRpcException ex3) {
				//Log
				logger.error("XMLRPC error",ex3);
				//Error
				send(JoinRoomResult.InternalServerError(messageId));
			} catch (IllegalArgumentException ex4) {
				//Error
				send(JoinRoomResult.BadRequest(messageId));
			} catch (ParserException ex5) {
				//Error
				send(JoinRoomResult.BadRequest(messageId));
			}
		} 
		else if (CommandType.LEAVE_ROOM_CMD.is(command) )
		{
			//Check we have participant
			if (participant==null)
			{
				//Error
				send(JoinRoomResult.BadRequest(messageId));
				//Exit
				return;
			}

			try {
				//Create client
				SFU.leaveRoom(participant);

				//Remove then
				participant = null;

				//Send back result
				send(new LeaveRoomResult(messageId));
			} catch (RoomNotFoundException ex2) {
				//Error
				send(LeaveRoomResult.BadRequest(messageId));
			}
		}
	}

	void terminate() {
		//Check we have room
		if ( participant==null)
			//Exit
			return;

		try {
			//Create client
			SFU.leaveRoom(participant);
		} catch (RoomNotFoundException ex) {
			logger.error("terminate participant error", ex);
		}
	}
}
