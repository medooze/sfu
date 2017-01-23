/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.GET;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import static io.netty.handler.codec.http.HttpHeaders.Names.ORIGIN;

public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

	private final static Logger logger = Logger.getLogger(WebSocketHandler.class);
	
	private String request;
	private String origin;
	private String ip;
	private Session session;
	WebSocketServerHandshaker handshaker;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) 
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		else if (msg instanceof WebSocketFrame)
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Exception caught", cause);
		release(ctx);
	}

	private static String getWebSocketLocation(FullHttpRequest req) {
		return "ws://" + req.headers().get(HOST) + "/sfu";
	}
	
	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			setContentLength(res, res.content().readableBytes());
		}

		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private void handleHttpRequest(final ChannelHandlerContext ctx, FullHttpRequest req) {
		//Check 
		if (!req.getDecoderResult().isSuccess()) 
		{
			//Send error response
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}

		//Only get methods allowd
		if (req.getMethod() != GET) {
			//Send error response
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED));
			return;
		}

		//Get url and origin IP
		origin = req.headers().get(ORIGIN);
		ip = req.headers().get("X-Forwarded-For");
		
		logger.info("Got WS request from IP " + ip + " and otigin " + origin);
		
		//Create WS handshaker
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
		handshaker = wsFactory.newHandshaker(req);
		
		//Check WS handshake compatibility
		if (handshaker == null)
		{
			//Not supported
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
			return;
		} 
		
		//Start handshake
		ChannelFuture future = handshaker.handshake(ctx.channel(), req);
		
		//Create session
		session = new Session(1L,ctx);
		
		//Listen for completion
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				logger.info("Handshake done"+ future.toString());
				//Check if it was sucesfull
				if (!future.isSuccess())
					//Release it
					release(ctx);
			}
		});
	}
	
	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		
		
		if (frame instanceof PongWebSocketFrame) 
		{
			return;
		}
		else if (frame instanceof CloseWebSocketFrame) 
		{
			//Close channel
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			release(ctx);
			return;
		}
		else if (frame instanceof PingWebSocketFrame) 
		{
			//Send pong
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		} else if (frame instanceof ContinuationWebSocketFrame) 
		{
			ContinuationWebSocketFrame continuation = (ContinuationWebSocketFrame) frame;
			request += continuation.aggregatedText();
			logger.info("Aggregated text is " + request);
			if (!continuation.isFinalFragment()) {
				logger.warn("Websocket secondary continuation detected! We will wait for next fragments. Text: " + continuation.text());
				return;
			}
		} else if (frame instanceof TextWebSocketFrame) {
			TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) frame;
			request = textWebSocketFrame.text();
			if (!textWebSocketFrame.isFinalFragment()) {
				logger.warn("Websocket continuation detected! We will wait for next fragments. Text: " + textWebSocketFrame.text());
				return;
			}
		} else {
			throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
		}
		
		
		logger.info("Websocket request on channel: " + request);

		try {
			//Convert to JSON
			JSONObject json = new JSONObject( request );
			//Process it
			session.onMessage(json);
		} catch (Exception ex) {
			logger.warn("Unhandled exception processing request",ex);
			//Stop on any exceptions
			release(ctx);
		}
        }

	private void release(ChannelHandlerContext ctx) {
		if (session!=null)
		{
			//terminate session
			session.terminate();
			//Null it
			session = null;
		}
		//Close contexg
		ctx.close();
	}
}
