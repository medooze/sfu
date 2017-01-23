/*
 * MediaMixer.java
 *
 * Copyright (C) 2007  Sergio Garcia Murillo
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.murillo.sfu;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.xmlrpc.XmlRpcException;
import org.murillo.MediaServer.XmlSFUClient;
import org.murillo.sfu.utils.PropertyContainer;
import org.murillo.sfu.utils.SubNetInfo;
/**
 *
 * @author Sergio Garcia Murillo
 */
@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class MediaMixer extends PropertyContainer implements Serializable,MediaMixerMCUEventQueue.Listener {
	private String uid;
	@XmlElement
	private String name;
	@XmlElement
	private String url;
	@XmlElement
	private String ip;
	@XmlElement
	private String publicIp;
	@XmlElement
	private SubNetInfo localNet;
	@XmlElement
	private Integer sys;
	@XmlElement
	private Integer user;
	@XmlElement
	private Integer load;
	@XmlElement
	private Integer cpus;
	@XmlElement
	private Integer loadAverage;
	
	private Boolean keepConnected;
	@XmlElement
	private Long idleSince;

	private MediaMixerMCUEventQueue eventQueue;
	private String state;
	private HashSet<XmlSFUClient> sfuClients;
	private Thread reconnectThread;
	private XmlSFUClient client;
	private Listener listener;
	
	private final static Logger logger = Logger.getLogger(MediaMixer.class.getName());

	public interface Listener  {
		public void onMediaMixerDisconnected(MediaMixer mediaMixer);
		public void onMediaMixerReconnected(MediaMixer mediaMixer);
	}
	
	public MediaMixer() {
		
	}

	public MediaMixer(String id,String name,String url,String ip,String publicIp,String localNet,String properties) throws MalformedURLException {
		///Create uuid
		this.uid = id;
		//Save Values
		this.name = name;
		//Check if it ends with "/"
		if (url.endsWith("/"))
			//Remove it
			this.url = url.substring(0,url.length()-2);
		else
			//Copy all
			this.url = url;
		this.ip = ip;
		this.publicIp = publicIp;
		//Create default client
		client = new XmlSFUClient(url + "/sfu");
		//Create client list
		sfuClients = new HashSet<XmlSFUClient>();
		//Set local net
		try {
			//parse it
			this.localNet = new SubNetInfo(localNet);
		} catch (UnknownHostException ex) {
			//Log
			logger.log(Level.SEVERE, "Wrong format for LocalNet expecified", ex);
			//Create empty one
			this.localNet = new SubNetInfo(new byte[]{0,0,0,0},0);
		}
		//Don't connect by default
		keepConnected = false;
		//NO event queue
		eventQueue = null;
		//NO state
		state = "";
		//No stats
		user = 0;
		sys = 0;
		load = 0;
		cpus = 0;
		loadAverage = -1;
		//Set them
		addProperties(properties);
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public void setUrl(String url) {
		//Check if it ends with "/"
		if (url.endsWith("/"))
			//Remove it
			this.url = url.substring(0,url.length()-2);
		else
			//Copy all
			this.url = url;
	}

	public void setLocalNet(String localNet) throws UnknownHostException {
		this.localNet = new SubNetInfo(localNet);
	}

	public void setListener(Listener listener) {
		//Set it
		this.listener = listener;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}
	
	public String getIp() {
		return ip;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public SubNetInfo getLocalNet() {
		return localNet;
	}

	public boolean isNated(String ip){
		try {
			//Check if it is a private network address  and not in local address
			if (SubNetInfo.isPrivate(ip) && !localNet.contains(ip))
				//It is nated
				return true;
		} catch (UnknownHostException ex) {
			//Log
			logger.log(Level.WARNING, "Wrong IP address, doing NAT {0}", ip);
			//Do nat
			return true;
		}

		//Not nat
		return false;
	}

	@XmlElement(name="id")
	public String getUID() {
		return uid;
	}
	
	public Integer getEventQueueId() {
		return eventQueue!=null?eventQueue.getId():0;
	}

	@XmlElement(name="isConnected")
	public Boolean isConnected() {
		return eventQueue!=null?eventQueue.isConnected():false;
	}

	

	public XmlSFUClient createSFUClient() {
		XmlSFUClient sfuClient = null;
		try {
			//Start event listener
			if (!startEventListener())
				//Error
				return null;
			//Create client
			sfuClient = new XmlSFUClient(url + "/sfu");
			//Append to set
			sfuClients.add(sfuClient);
			//Not idle
			idleSince = -1L;
		} catch (MalformedURLException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		//Return client
		return sfuClient;
	}

	private synchronized boolean  startEventListener() throws MalformedURLException {
		//If we have  an event queue
		if (eventQueue!=null)
			//Started already
			return true;
		try {
			//Create event queue
			Integer queueId = client.EventQueueCreate();
			//Attach
			eventQueue = new MediaMixerMCUEventQueue(queueId,url+"/events/sfu/"+Integer.toString(queueId));
			//Set listener
			eventQueue.setListener(this);
			//Start listening for events
			eventQueue.start();
			//Log
			logger.log(Level.FINEST, "Started event listener queueId:{0}",queueId);
		} catch (XmlRpcException ex) {
			//Log error
			logger.log(Level.SEVERE, "Exception trying to start event listener {0}", ex.getMessage());
			//Start reconnection
			startRetryConnect();
			//Not started
			return false;
		}
		
		//OK
		return true;
	}

	private synchronized void stopEventListener() {
		//Check
		 if (eventQueue==null)
			 //Do nothing
			 return;
		//Log
		logger.log(Level.FINEST, "Stop event listener");
		//Stop waiting for events
		eventQueue.stop();
		try{
			//Delete event queue
			client.EventQueueDelete(eventQueue.getId());
			//Log
		} catch (XmlRpcException ex) {
			logger.log(Level.SEVERE, "Exception deleting event queue {0}", ex.getMessage());
		}
		//Clean
		eventQueue = null;
	}

	protected synchronized void startRetryConnect() {
		//Check if thread already running
		if (reconnectThread!=null && reconnectThread.isAlive())
		{
			//Notify it so it retries now
			notifyAll();
			//Exit
			return;
		}
		//We
		final MediaMixer mediaMixer = this;
		//Start reconnecting thread
		reconnectThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				//Set state
				state = "Connecting";
				//Initial time to sleep
				int sleep = 1;
				//Log
				logger.log(Level.FINEST, "MediaMixer re-connecting");
				//Try until interrupted
				while(Thread.currentThread().equals(reconnectThread))
				{
					try {
						//Stop previous event listener
						stopEventListener();
						//Try to connect
						if (startEventListener())
						{
							//Check if we have listeners
							if (listener!=null)
								//Send event
								listener.onMediaMixerReconnected(mediaMixer);
							//done
							return;
						}
						//Check if lower than maximium retry time
						if (sleep<64)
							//Duplicate time
							sleep*=2;
						//Log
						logger.log(Level.FINEST, "MediaMixer reconnect failed retrying in {0} sec", new Object[]{sleep});
						
					} catch (MalformedURLException ex2) {
						//Log
						logger.log(Level.FINEST, "MediaMixer reconnect failed", ex2);
						//Exit
						return;
					}
					try {
						//Lock
						synchronized(mediaMixer) {
							//Wait
							mediaMixer.wait(sleep*1000);
						}
					} catch (InterruptedException ex) {
						//Log
						logger.log(Level.FINEST, "MediaMixer reconnect interrupted");
						//Exit
						continue;
					}
				}
			}
		});

		//Start thread
		reconnectThread.start();
	}

	private synchronized void stopRetryConnect() {
	//remove thread
	reconnectThread = null;
	//Signal
	this.notifyAll();
	}

	
	@Override
	public void onMCUEventQueueConnected() {
		//Stop any pending reconnect, just in case
		stopRetryConnect();
		//Set state
		state = "Connected";
		//Log
		logger.log(Level.INFO, "MediaMixer sfu event queue connected [id:{0}]",getUID());
	}

	@Override
	public void onMCUEventQueueDisconnected() {
		//Set state
		state = "Disconnected";
		//Log
		logger.log(Level.SEVERE, "MediaMixer sfu event queue disconnected [id:{0},queueId:{1}]", new Object[]{getUID(),getEventQueueId()});
		//Fire listener
		if (listener!=null)
			listener.onMediaMixerDisconnected(this);
	}

	@Override
	public void onMCUEventQueueError() {
		//Set errror
		state = "Error";
		//Log
		logger.log(Level.SEVERE, "MediaMixer sfu error [id:{0},queueId:{1}]", new Object[]{getUID(),getEventQueueId()});
		//Start reconnecting
		startRetryConnect();
	}

	public String getState() {
		return state;
	}

	void releaseMcuClient(XmlSFUClient client) {
		//Release client
		sfuClients.remove(client);
		//Check number of clients
		if (sfuClients.isEmpty())
		{
			//Set idle
			idleSince = System.currentTimeMillis();
			//Check if we need to keep it connected
			if (!keepConnected)
				//Stop event listener
				stopEventListener();
		}
	}
	
	@Override
	public void onCPULoadInfo(Integer user, Integer sys, Integer load, Integer cpus) {
		//Store values
		this.user = user;
		this.sys  = sys;
		this.load = load;
		this.cpus = cpus;
		//Calculate load average
		if (loadAverage==-1)
			//First
			loadAverage = load;
		else
			//median
			loadAverage = (int)(0.9*loadAverage + 0.1*load);
		}

	public Integer getCpus() {
		return cpus;
	}

	public Integer getLoad() {
		return load;
	}

	public Integer getLoadAverage() {
		return loadAverage;
	}

	public Integer getSystemLoad() {
		return sys;
	}

	public Integer getUserLoad() {
		return user;
	}

	@XmlElement
	public int getScore() {
		return (int) (((100 - load) * cpus)/(1.0+((float)sfuClients.size())/5));
	}
	
	@XmlElement
	public int getUsage() {
		return sfuClients.size();
	}
 
	@XmlElement(name="isIdle")
	public boolean isIdle() {
		return sfuClients.isEmpty() && idleSince>0;
	}
	
	@XmlElement(name="cooldown")
	public Long getCoolDown() {
		//Check if it is idle
		if (idleSince>0)
			//Return number of seconds
			return (System.currentTimeMillis()-idleSince)/1000;
		//Not idle	
		return -1L;
	}
	
	public synchronized void connect() {
		//Keep connected even with no runing confs
		keepConnected = true;
		//Connect async
		startRetryConnect();
	}
	public synchronized void disconnect() {
		//Disconnect when not used
		keepConnected = false;
		//Stop eny reconnection
		stopRetryConnect();
		//Check number of clients
		if (sfuClients.isEmpty())
			//Stop event listener
			stopEventListener();
	}
	
}
