/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.Future;
import java.net.MalformedURLException;
import org.apache.log4j.BasicConfigurator;

import org.apache.log4j.Logger;

public class Server implements Runnable {

	private static Server instance;
	private static Thread thread;
	private static final int DEFAULT_PORT = 8084;
	private static final Logger logger = Logger.getLogger(Server.class);
	private Integer port;

	public static void start(Integer port) {
		logger.info("Starting the SFU Server");

		if (instance != null) {
			return;
		}
		//Create instance
		instance = new Server(port);
		thread = new Thread(instance);
		//Start it
		thread.start();
	}

	public static void stop() {
		//Stop instance
		if (thread!=null) thread.interrupt();
	}

	private Server(Integer port) {
		this.port = port;
	}

	@Override
	public void run() {
		logger.info("Starting SFU websocket server");

		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			ServerBootstrap child = b.group(bossGroup, workerGroup);
			ServerBootstrap channel = child.channel(NioServerSocketChannel.class);
			channel.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("codec-http", new HttpServerCodec());
					pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
					pipeline.addLast("handler", new WebSocketHandler());
				}
			});

			Channel ch = b.bind(port).sync().channel();
			logger.info("SFU websocket server started at port " + port + '.');
			ch.closeFuture().sync();
		} catch (Exception ex) {
			//Log it
			logger.error("Error starting chat server", ex);
		} finally {
			
			Future fb = bossGroup.shutdownGracefully();
			Future fw = workerGroup.shutdownGracefully();

			try {
				fb.await();
				fw.await();
			} catch (InterruptedException ignore) {
			}
		}
		logger.info("Chat weebsocket server stopped");
	}

	public static void main(String[] args) throws MalformedURLException {
	
		// Set up a simple configuration that logs on the console.
		BasicConfigurator.configure();
		
		//Default mixer properties
		String url = "http://192.168.64.129:8080";
		String ip = "192.168.64.129";
		String publicIp = "192.168.64.129";
		String subnet = "0.0.0.0/32";
		
		//Create Media mixer
		MediaMixer mixer = new MediaMixer("mixer", "mixer",url, ip, publicIp, subnet, "");
		
		//Start SFU
		SFU.init(mixer);

		try {
			start(DEFAULT_PORT);
			
			synchronized(instance)
			{
				instance.wait();
			}
		} catch (InterruptedException ex) {
			
		}
		
		//Stop SFU
		SFU.terminate();
	}

}
