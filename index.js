const TransactionManager = require("transaction-manager");
const Room		 = require("./lib/Room");
//Get Semantic SDP objects
const SemanticSDP	= require("semantic-sdp");
const SDPInfo		= SemanticSDP.SDPInfo;

const PORT = 8084;

//HTTP&WS stuff
const https = require ('https');
const url = require ('url');
const fs = require ('fs');
const path = require ('path');
const WebSocketServer = require ('websocket').server;

//Get the Medooze Media Server interface
const MediaServer = require("medooze-media-server");

//Enable debug
MediaServer.enableLog(false);
MediaServer.enableDebug(false);
MediaServer.enableUltraDebug(false);

//Check 
if (process.argv.length!=3)
	 throw new Error("Missing IP address\nUsage: node index.js <ip>"+process.argv.length);
//Get ip
const ip = process.argv[2];

//The list of sport castings
const rooms = new Map();

const base = 'www';

const options = {
	key: fs.readFileSync ('server.key'),
	cert: fs.readFileSync ('server.cert')
};

// maps file extention to MIME typere
const map = {
	'.ico': 'image/x-icon',
	'.html': 'text/html',
	'.js': 'text/javascript',
	'.json': 'application/json',
	'.css': 'text/css',
	'.png': 'image/png',
	'.jpg': 'image/jpeg',
	'.wav': 'audio/wav',
	'.mp3': 'audio/mpeg',
	'.svg': 'image/svg+xml',
	'.pdf': 'application/pdf',
	'.doc': 'application/msword'
};


//Create HTTP server
const server = https.createServer (options, (req, res) => {
	// parse URL
	const parsedUrl = url.parse (req.url);
	// extract URL path
	let pathname = base + parsedUrl.pathname;
	// based on the URL path, extract the file extention. e.g. .js, .doc, ...
	const ext = path.parse (pathname).ext;

	//DO static file handling
	fs.exists (pathname, (exist) => {
		if (!exist)
		{
			// if the file is not found, return 404
			res.statusCode = 404;
			res.end (`File ${pathname} not found!`);
			return;
		}

		// if is a directory search for index file matching the extention
		if (fs.statSync (pathname).isDirectory ())
			pathname += '/index.html';

		// read file from file system
		fs.readFile (pathname, (err, data) => {
			if (err)
			{
				//Error
				res.statusCode = 500;
				res.end (`Error getting the file: ${err}.`);
			} else {
				// if the file is found, set Content-type and send data
				res.setHeader ('Content-type', map[ext] || 'text/html');
				res.end (data);
			}
		});
	});
}).listen (PORT);

//Create ws server
const ws = new WebSocketServer ({
	httpServer: server,
	autoAcceptConnections: false
});

//Listen for requests
ws.on ('request', (request) => {
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
						tm.event("participants", participants);
					}));
					
					//Get all streams before adding us
					const streams = room.getStreams();
					
					//Init participant
					const answer = participant.init(data.sdp);
					
					//Accept cmd
					cmd.accept({
						sdp	: answer,
						room	: room.getInfo()
					});
					
					participant.on("renegotiationneeded",async (sdp) => {
						//Send update event
						const answer = await tm.cmd('update',{
							sdp	: sdp
						});
						//Update partitipant
						participant.update(answer.sdp);
					});
					
					//listen for participant events
					participant.on("closed",function(){
						//close ws
						connection.close();
						//Remove room listeners
						room.off("participants",updateParticipants);
					});
					
					//For each one
					for (let stream of streams)
						//Add it
						participant.addStream(stream);
					
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
});
