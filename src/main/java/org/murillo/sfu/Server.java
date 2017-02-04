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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;
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
		// Set up a simple log configuration that logs on the console.
		BasicConfigurator.configure();
		
		//Get configuration file
		String configFile = "sfu.conf";
		    
		//Load configuration
		Properties config = new Properties();
		
		//Default server properties
		Integer port = DEFAULT_PORT;
		
		//Default mixer properties
		String url = "http://192.168.64.129:8080";
		String ip = "192.168.64.129";
		String publicIp = "192.168.64.129";
		String subnet = "0.0.0.0/32";
	
		try {
			//Load from file
			config.load( new FileInputStream(configFile));
			//Get mixer url
			if (config.containsKey("mixer.url"))
				//Set it
				url = config.getProperty("mixer.url");
			//Get mixer url
			if (config.containsKey("mixer.ip"))
				//Set it
				ip = config.getProperty("mixer.url");
			//Get mixer url
			if (config.containsKey("mixer.publicIp"))
				//Set it
				publicIp = config.getProperty("mixer.publicIp");
			//Get mixer url
			if (config.containsKey("mixer.subnet"))
				//Set it
				subnet = config.getProperty("mixer.subnet");
		
			//Check domain
			if (config.containsKey("server.port"))
				//Get value
				port = Integer.parseInt(config.getProperty("server.port"));
			
		} catch (FileNotFoundException ex) {
			logger.warn("Configuration file not found on "+configFile+" setting defaults");
		} catch (IOException ex) {
			logger.error("Cannot read configuration file on "+configFile+", setting defaults", ex);
		} catch (Exception ex) {
			logger.error("Cannot read configuration file on "+configFile+", setting defaults", ex);
		}
		
		//Create Media mixer
		MediaMixer mixer = new MediaMixer("mixer", "mixer",url, ip, publicIp, subnet, "");
		
		//Log 
		logger.info("Using media server on "+url+" with public ip:"+publicIp);
		
		//Start SFU
		SFU.init(mixer);

		try {
			start(port);
			
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
