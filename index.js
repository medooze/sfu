const Express			= require("express");
const BodyParser		= require("body-parser");
const FS			= require("fs");
const HTTPS			= require("https");
const Path			= require("path");
const WebSocketServer		= require("websocket").server;
const TransactionManager	= require("transaction-manager");
const Room			= require("./lib/Room");
//Get Semantic SDP objects
const SemanticSDP		= require("semantic-sdp");
const SDPInfo			= SemanticSDP.SDPInfo;

const PORT = 8084;
const letsencrypt = true;

//Get the Medooze Media Server interface
const MediaServer = require("medooze-media-server");

//Enable debug
MediaServer.enableDebug(false);
MediaServer.enableUltraDebug(false);

//Check 
if (process.argv.length!=3)
	 throw new Error("Missing IP address\nUsage: node index.js <ip>"+process.argv.length);
//Get ip
const ip = process.argv[2];

//The list of sport castings
const rooms = new Map();

//Create rest api
const rest = Express();
rest.use(function(req, res, next) {
	res.header("Access-Control-Allow-Origin", "*");
	res.header("Access-Control-Allow-Headers", "*");
	res.header("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT, OPTIONS");
	next();
});
rest.use(BodyParser.text({type:"application/sdp"}));
rest.post("/whip/:roomId" , (req, res)=>{
	//Get body
	const body = req.body;
	//Get streamId
	const roomId = req.params.roomId;
	//Get room
	const room = rooms.get(roomId);
	
	//if not found
	if (!room) 
		return res.sendStatus(404);
	
	//Create it
	const publication = room.createPublication();

	//Check
	if (!publication)
		return res.sendStatus(400, "Error creating participant");

	//Process the sdp
	const offer = SDPInfo.process(body);

	//Publish 
	const answer = publication.init(offer);

	//Done
	res.type("application/sdp");
	res.send(answer.toString());
	
});

rest.use(Express.static("www"));

//Listen for ws requests
function proccessRequest(request) 
{
	// parse URL
	const url = request.resourceURL;
	
	//Find the room id
	let updateParticipants;
	let participant;
	let room = rooms.get(url.query.id);
	
	//if not found
	if (!room) 
	{
		//Create new Room
		room = new Room(url.query.id,ip);
		//Append to room list
		rooms.set(room.getId(), room);
	}
	
	//Get protocol
	var protocol = request.requestedProtocols[0];
	
	//Accept the connection
	const connection = request.accept(protocol);
	
	//Create new transaction manager
	const tm = new TransactionManager(connection);

	//Handle incoming commands
	tm.on("cmd", async function(cmd) 
	{
		//Get command data
		const data = cmd.data;
		//check command type
		switch(cmd.name)
		{
			case "join":
				try {
					//Check if we already have a participant
					if (participant)
						return cmd.reject("Already joined");

					//Create it
					participant = room.createParticipant(data.name);
					
					//Check
					if (!participant)
						return cmd.reject("Error creating participant");

					//Add listener
					room.on("participants",(updateParticipants = (participants) => {
						console.log("room::participants");
						tm.event("participants", {
							participants	: participants,
							streams		: participant.getOutgoingStreamsMapping() 
						});
					}));
					
					//Add listener
					room.on("publications",(updatePublications = (publications) => {
						console.log("room::publicastions");
						tm.event("publications", {
							publications	: publications,
							streams		: participant.getOutgoingStreamsMapping() 
						});
					}));
					
					//Process the sdp
					const sdp = SDPInfo.process(data.sdp);
		
					//Get all streams before adding us
					const streams = room.getStreams();
					
					//Init participant
					participant.init(sdp);
					
					//For each one
					for (let [stream,other] of streams)
						//Add it
						participant.addStream(stream,other);
					
					//Get answer
					const answer = participant.getLocalSDP();

					//Accept cmd
					cmd.accept({
						sdp		: answer.toString(),
						room		: room.getInfo(),
						streams		: participant.getOutgoingStreamsMapping() 
					});
					
					//For all remote streams
					for (let stream of sdp.getStreams().values())
						//Publish them
						participant.publishStream(stream);
					
					participant.on("renegotiationneeded",(sdp) => {
						console.log("participant::renegotiationneeded");
						//Send update event
						tm.event("update",{
							sdp		: sdp.toString(),
							streams		: participant.getOutgoingStreamsMapping() 
						});
					});
					
					//listen for participant events
					participant.on("closed",function(){
						//close ws
						connection.close();
						//Remove room listeners
						room.off("participants",updateParticipants);
						room.off("publications",updatePublications);
					});
					
				} catch (error) {
					console.error(error);
					//Error
					cmd.reject({
						error: error
					});
				}
				break;
		}
	});

	connection.on("close", function(){
		console.log("connection:onclose");
		//Check if we had a participant
		if (participant)
			//remove it
			participant.stop();
	});
}

function wss(server)
{
	//Create websocket server
	const wssServer = new WebSocketServer ({
		httpServer: server,
		autoAcceptConnections: false
	});

	wssServer.on ("request", request => proccessRequest(request));
}

//Create HTTP server
if (letsencrypt)
{
	//Use greenlock to get ssl certificate
	const gle = require("greenlock-express").init({
			packageRoot: __dirname,
			configDir: "./greenlock.d",
			maintainerEmail : "sergio.garcia.murillo@gmail.com",
			cluster: false
		});
	gle.ready((gle)=>wss(gle.httpsServer()));
	gle.serve(rest);
} else {
	//Load certs
	const options = {
		key	: FS.readFileSync ("server.key"),
		cert	: FS.readFileSync ("server.cert")
	};
	
	//Manualy starty server
	const server = HTTPS.createServer (options, rest).listen(PORT);
	
	//Launch wss server
	wss(server);
}
