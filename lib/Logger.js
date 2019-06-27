process.env["DEBUG"]="*,-websocket:connection";
const Debug = require("debug");

Debug.formatArgs = function (args) {
	var name = this.namespace;
	var useColors = this.useColors;

	if (useColors) {
		var c = this.color;
		var colorCode = '\u001b[3' + (c < 8 ? c : '8;5;' + c);
		var prefix = colorCode + ';1m' +  new Date().toISOString()  + name + ' ';

		args[0] = prefix + args[0].split ('\n').join ('\u001b[0m\n' + prefix) + '\u001b[0m';
	} else {
		args[0] = new Date().toISOString() + name + ' ' + args[0];
	}
};

class Logger
{
	constructor(name)
	{
		this.name	= name;
		this.info	= Debug(" [INFO ] "+name);
		this.debug	= Debug(" [DEBUG] "+name);
		this.warn	= Debug(" [WARN ] "+name);
		this.error	= Debug(" [ERROR] "+name);
	}
	
	child(name)
	{
		return new Logger(this.name +"::"+name);
	}
};

module.exports = Logger;

