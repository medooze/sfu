let participants;
let audioDeviceId;
let videoResolution = true;

//Get our url
const href = new URL(window.location.href);
//Get id
const roomId = href.searchParams.get("roomId");
//Get name
const name = href.searchParams.get("name");
//Get key
const key = href.searchParams.get("key");
//Get video
const nopublish = href.searchParams.has("nopublish");
//Get ws url from navigaro url
const url = "wss://"+href.host;
//Check support for insertabe media streams
// In Chrome v98 is "RTCRtpSender.prototype.createEncodedStreams"
const supportsInsertableStreams = !!RTCRtpSender.prototype.createEncodedVideoStreams || !!RTCRtpSender.prototype.createEncodedStreams;

if (href.searchParams.has ("video"))
	switch (href.searchParams.get ("video").toLowerCase ())
	{
		case "1080p":
			videoResolution = {
				width: {min: 1920, max: 1920},
				height: {min: 1080, max: 1080},
			};
			break;
		case "720p":
			videoResolution = {
				width: {min: 1280, max: 1280},
				height: {min: 720, max: 720},
			};
			break;
		case "576p":
			videoResolution = {
				width: {min: 720, max: 720},
				height: {min: 576, max: 576},
			};
			break;
		case "480p":
			videoResolution = {
				width: {min: 640, max: 640},
				height: {min: 480, max: 480},
			};
			break;
		case "320p":
			videoResolution = {
				width: {min: 320, max: 320},
				height: {min: 240, max: 240},
			};
			break;
		case "no":
			videoResolution = false;
			break;
	}


function addRemoteTrack(event)
{
	console.log(event);
	
	const track	= event.track;
	const stream	= event.streams[0];
	
	if (!stream)
		return console.log("addRemoteTrack() no stream")
	
	//Check if video is already present
	let video = container.querySelector("video[id='"+stream.id+"']");
	
	//Check if already present
	if (video)
		//Ignore
		return console.log("addRemoteTrack() video already present for "+stream.id);
	
	//Listen for end event
	track.onended=(event)=>{
		console.log(event);
	
		//Check if video is already present
		let video = container.querySelector("video[id='"+stream.id+"']");

		//Check if already present
		if (!video)
			//Ignore
			return console.log("removeRemoteTrack() video not present for "+stream.id);

		container.removeChild(video);
	}
	
	//Create new video element
	video = document.createElement("video");
	//Set same id
	video.id = stream.id;
	//Set src stream
	video.srcObject = stream;
	//Set other properties
	video.autoplay = true;
	video.play();
	//Append it
	container.appendChild(video);
}
	
function addLocalVideoForStream(stream,muted)
{
	//Create new video element
	const video = document.createElement("video");
	//Set same id
	video.id = stream.id;
	//Set src stream
	video.srcObject = stream;
	//Set other properties
	video.autoplay = true;
	video.muted = muted;
	video.play();
	//Append it
	container.appendChild(video);
}

/*
 Get some key material to use as input to the deriveKey method.
 The key material is a secret key supplied by the user.
 */
async function getRoomKey(roomId,secret) 
{
	const enc = new TextEncoder();
	const keyMaterial = await window.crypto.subtle.importKey(
		"raw",
		enc.encode(secret),
		{name: "PBKDF2"},
		false,
		["deriveBits", "deriveKey"]
	);
	return window.crypto.subtle.deriveKey(
		{
			name: "PBKDF2",
			salt: enc.encode(roomId),
			iterations: 100000,
			hash: "SHA-256"
		},
		keyMaterial,
		{"name": "AES-GCM", "length": 256},
		true,
		["encrypt", "decrypt"]
	);
}

  /*
   * 
   */
async function connect(url,roomId,name,secret) 
{
	let counter = 0;
	const roomKey = await getRoomKey(roomId,secret);
	async function encrypt(chunk, controller) {
		try {
			//Get iv
			const iv = new ArrayBuffer(4);
			//Create view, inc counter and set it
			new DataView(iv).setUint32(0,counter <65535 ? counter++ : counter=0);
			//Encrypt
			const ciphertext = await window.crypto.subtle.encrypt(
				{
					name: "AES-GCM",
					iv: iv
				},
				roomKey,
				chunk.data
			);
			//Set chunk data
			chunk.data = new ArrayBuffer(ciphertext.byteLength + 4);
			//Crate new encoded data and allocate size for iv
			const data = new Uint8Array(chunk.data);
			//Copy iv
			data.set(new Uint8Array(iv),0);
			//Copy cipher
			data.set(new Uint8Array(ciphertext),4);
			//Write
			controller.enqueue(chunk);
		} catch(e) {
		}
	}

	async function decrypt(chunk, controller) {
		try {
			//decrypt
			chunk.data =  await window.crypto.subtle.decrypt(
				{
				  name: "AES-GCM",
				  iv: new Uint8Array(chunk.data,0,4)
				},
				roomKey,
				new Uint8Array(chunk.data,4,chunk.data.byteLength - 4)
			);
			//Write
			controller.enqueue(chunk);
		} catch(e) {
		}
	}
	
	const isCryptoEnabled = !!secret && supportsInsertableStreams;

	var pc = new RTCPeerConnection({
		bundlePolicy				: "max-bundle",
		rtcpMuxPolicy				: "require",
		forceEncodedVideoInsertableStreams	: isCryptoEnabled
	});
	
	//Create room url
	const roomUrl = url +"?id="+roomId;
		
	var ws = new WebSocket(roomUrl);
	var tm = new TransactionManager(ws);
	
	pc.ontrack = (event) => {
		//If encrypting/decrypting
		if (isCryptoEnabled) 
		{
			//Create transfor strem fro decrypting
			const transform = new TransformStream({
				start() {},
				flush() {},
				transform: decrypt
			});
			//Get the receiver streams for track
			let receiverStreams = event.receiver.createEncodedVideoStreams();
			//Decrytp
			receiverStreams.readableStream
				.pipeThrough(transform)
				.pipeTo(receiverStreams.writableStream);
		}
		addRemoteTrack(event);
	};
	
	ws.onopen = async function()
	{
	        console.log("ws:opened");
		
		try
		{
			if (!nopublish)
			{
				const stream = await navigator.mediaDevices.getUserMedia({
					audio: {
						deviceId: audioDeviceId
					},
					video: videoResolution
				});

				console.debug("md::getUserMedia sucess",stream);

				//Play it
				addLocalVideoForStream(stream,true);
				//Add stream to peer connection
				for (const track of stream.getTracks())
				{
					//Add track
					const sender = pc.addTrack(track,stream);
					//If encrypting/decrypting
					if (isCryptoEnabled) 
					{
						//Get insertable streams
						const senderStreams = sender.createEncodedVideoStreams();
						//Create transform stream for encryption
						let senderTransformStream = new TransformStream({
							start() {},
							flush() {},
							transform: encrypt
						});
						//Encrypt
						senderStreams.readableStream
						    .pipeThrough(senderTransformStream)
						    .pipeTo(senderStreams.writableStream);
					}
  				}
			 }
			
			//Create new offer
			const offer = await pc.createOffer({
				offerToReceiveAudio: true,
				offerToReceiveVideo: true
			});

			console.debug("pc::createOffer sucess",offer);

			//Set it
			pc.setLocalDescription(offer);

			console.log("pc::setLocalDescription succes",offer.sdp);
			
			//Join room
			const joined = await tm.cmd("join",{
				name	: name,
				sdp	: offer.sdp
			});
			
			console.log("cmd::join success",joined);
			
			//Create answer
			const answer = new RTCSessionDescription({
				type	:'answer',
				sdp	: joined.sdp
			});
			
			//Set it
			await pc.setRemoteDescription(answer);
			
			console.log("pc::setRemoteDescription succes",answer.sdp);
			
			console.log("JOINED");
		} catch (error) {
			console.error("Error",error);
			ws.close();
		}
	};
	
	tm.on("cmd",async function(cmd) {
		console.log("ts::cmd",cmd);
		
		switch (cmd.name)
		{
			case "update" :
				try
				{
					console.log(cmd.data.sdp);
					
					//Create new offer
					const offer = new RTCSessionDescription({
						type : 'offer',
						sdp  : cmd.data.sdp
					});
					
					//Set offer
					await pc.setRemoteDescription(offer);
					
					console.log("pc::setRemoteDescription succes",offer.sdp);
					
					//Create answer
					const answer = await pc.createAnswer();
					
					console.log("pc::createAnswer succes",answer.sdp);
					
					//Only set it locally
					await pc.setLocalDescription(answer);
					
					console.log("pc::setLocalDescription succes",answer.sdp);
					
					//accept
					cmd.accept({sdp:answer.sdp});
					
				} catch (error) {
					console.error("Error",error);
					ws.close();
				}
				break;
		}
	});
	
	tm.on("event",async function(event) {
		console.log("ts::event",event);
		
		switch (event.name)
		{
			case "participants" :
				//update participant list
				participants = event.participants;
				break;	
		}
	});
}

navigator.mediaDevices.getUserMedia({
	audio: true,
	video: false
})
.then(function(stream){	

	//Set the input value
	audio_devices.value = stream.getAudioTracks()[0].label;
	
	//Get the select
	var menu = document.getElementById("audio_devices_menu");
	
	//Populate the device lists
	navigator.mediaDevices.enumerateDevices()
		.then(function(devices) {
			//For each one
			devices.forEach(function(device) 
			{
				//It is a mic?
				if (device.kind==="audioinput")
				{
					//Create menu item
					var li = document.createElement("li");
					//Populate
					li.dataset["val"] = device.deviceId;	
					li.innerText = device.label;
					li.className = "mdl-menu__item";
					
					//Add listener
					li.addEventListener('click', function() {
						console.log(device.deviceId);
						//Close previous
						stream.getAudioTracks()[0].stop();
						//Store device id
						audioDeviceId = device.deviceId
						//Get stream for the device
						navigator.mediaDevices.getUserMedia({
							audio: {
								deviceId: device.deviceId
							},
							video: false
						})
						.then(function(stream){	
							//Store it
							soundMeter.connectToSource(stream).then(draw);
						});
	
					});
					//Append
					menu.appendChild (li);
				}
			});
			//Upgrade
			getmdlSelect.init('.getmdl-select');
		        componentHandler.upgradeDom();
		})
		.catch(function(error){
			console.log(error);
		});
	
	var fps = 20;
	var now;
	var then = Date.now();
	var interval = 1000/fps;
	var delta;
	var drawTimer;
	var soundMeter = new SoundMeter(window);
	//Stop
	cancelAnimationFrame(drawTimer);

	function draw() {
		drawTimer = requestAnimationFrame(draw);

		now = Date.now();
		delta = now - then;

		if (delta > interval) {
			then = now ;
			var tot = Math.min(100,(soundMeter.instant*200));
			//Get all 
			const voometers = document.querySelectorAll (".voometer");
			//Set new size
			for (let i=0;i<voometers.length;++i)
				voometers[i].style.width = (Math.floor(tot/5)*5) + "%";
		}
	
	}
	soundMeter.connectToSource(stream).then(draw);
	
	var dialog = document.querySelector('dialog');
	dialog.showModal();
	if (!supportsInsertableStreams)
		dialog.querySelector('#key').parentElement.innerHTML = "<red>Your browser does not support insertable streams<red>";
	if (roomId)
	{
		dialog.querySelector('#roomId').parentElement.MaterialTextfield.change(roomId);
		supportsInsertableStreams && dialog.querySelector('#key').parentElement.MaterialTextfield.change(key);
		dialog.querySelector('#name').focus();
	}
	dialog.querySelector('#random').addEventListener('click', function() {
		dialog.querySelector('#roomId').parentElement.MaterialTextfield.change(Math.random().toString(36).substring(7));
		dialog.querySelector('#name').parentElement.MaterialTextfield.change(Math.random().toString(36).substring(7));
		dialog.querySelector('#key').parentElement.MaterialTextfield.change(Math.random().toString(36).substring(7));
	});
	dialog.querySelector('form').addEventListener('submit', function(event) {
		dialog.close();
		var a = document.querySelector(".room-info a");
		a.target = "_blank";
		a.href = "?roomId="+this.roomId.value;
		if (this.key.value)
			a.href += "&key="+encodeURI(this.key.value);
		a.innerText = this.roomId.value;
		a.parentElement.style.opacity = 1;
		connect(url, this.roomId.value, this.name.value,this.key.value);
		event.preventDefault();
	});
});

