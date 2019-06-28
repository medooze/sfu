let participants;
let audioDeviceId;

//Get our url
const href = new URL(window.location.href);
//Get id
const roomId = href.searchParams.get("roomId");
//Get name
let name = href.searchParams.get("name");
if (!name) {
	name = href.searchParams.get("username");
}
//Get video
const nopublish = href.searchParams.has("nopublish");
//Get ws url from navigaro url
const url = "wss://"+href.host;


function addVideoForStream(stream,muted)
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
	//Append it
	container.appendChild(video);
}

function removeVideoForStream(stream)
{
	//Get video
	var video = document.getElementById(stream.id);
	//Remove it when done
	video.addEventListener('webkitTransitionEnd',function(){
			//Delete it
		video.parentElement.removeChild(video);
		});
	//Disable it first
	video.className = "disabled";
}

function connect(url,roomId,name)
{
	var pc = window.pc = new RTCPeerConnection({
		bundlePolicy: "max-bundle",
		rtcpMuxPolicy : "require",
		sdpSemantics	: "plan-b"
	});

	let getStats = new GetStats();
	getStats.init("//logstash.cosmosoftware.io:5000", name, roomId, "Medooze", pc); //Use "http://127.0.0.1:5000" to use it locally with logstash on the client side
	getStats.startPublishing(1000);

	//Create room url
	const roomUrl = url +"?id="+roomId;

	var ws = new WebSocket(roomUrl);
	var tm = new TransactionManager(ws);

	pc.onaddstream = function(event) {
		console.debug("pc::onAddStream",event);
		//Play it
		addVideoForStream(event.stream);
	};

	pc.onremovestream = function(event) {
		console.debug("pc::onRemoveStream",event);
		//Play it
		removeVideoForStream(event.stream);
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
					video: true
				});

				console.debug("md::getUserMedia sucess",stream);

				//Play it
				addVideoForStream(stream,true);
				//Add stream to peer connection
				 pc.addStream(stream);
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

	tm.on("event",async function(event) {
		console.log("ts::event",event);

		switch (event.name)
		{
			case "update" :
				try
				{
					console.log(event.data.sdp);

					//Create new offer
					const offer = new RTCSessionDescription({
						type : 'offer',
						sdp  : event.data.sdp
					});

					//update participant list
					participants = event.participants;

					//Set offer
					await pc.setRemoteDescription(offer);

					console.log("pc::setRemoteDescription succes",offer.sdp);

					//Create answer
					const answer = await pc.createAnswer();

					console.log("pc::createAnswer succes",answer.sdp);

					//Only set it locally
					await pc.setLocalDescription(answer);

					console.log("pc::setLocalDescription succes",answer.sdp);

				} catch (error) {
					console.error("Error",error);
					ws.close();
				}
				break;
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
	if (roomId && name) {
		dialog.querySelector('#roomId').parentElement.MaterialTextfield.change(roomId);
		dialog.close();
		var a = document.querySelector(".room-info a");
		a.target = "_blank";
		a.href = "?roomId="+this.roomId.value;
		a.innerText = this.roomId.value;
		a.parentElement.style.opacity = 1;
		connect(url, this.roomId.value, name);
		event.preventDefault();
	}
	else {
		if (roomId) {
			dialog.querySelector('#roomId').parentElement.MaterialTextfield.change(roomId);
			dialog.querySelector('#name').focus();
		}
		else {
			dialog.querySelector('#random').addEventListener('click', function() {
				dialog.querySelector('#roomId').parentElement.MaterialTextfield.change(Math.random().toString(36).substring(7));
				dialog.querySelector('#name').parentElement.MaterialTextfield.change(Math.random().toString(36).substring(7));
			});
		}
		dialog.querySelector('form').addEventListener('submit', function(event) {
			dialog.close();
			var a = document.querySelector(".room-info a");
			a.target = "_blank";
			a.href = "?roomId="+this.roomId.value;
			a.innerText = this.roomId.value;
			a.parentElement.style.opacity = 1;
			connect(url, this.roomId.value, this.name.value);
			event.preventDefault();
		});
	}
});
