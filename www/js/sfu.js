const url = "wss://192.168.64.129:8000/ws";
let participants;

//Get our url
const href = new URL(window.location.href);
//Get id
const roomId = href.searchParams.get("roomId") || 1;
//Get name
const name = href.searchParams.get("name") || "<anonymous>";
//Get video
const nopublish = href.searchParams.has("nopublish");

	
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

function connect(url,roomId) 
{
	var pc = new RTCPeerConnection({
		bundlePolicy: "max-bundle",
		rtcpMuxPolicy : "require"
	});
	
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
					audio: true,
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
				name	: "pepe",
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


connect(url, roomId);



