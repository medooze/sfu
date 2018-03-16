(function(f){if(typeof exports==="object"&&typeof module!=="undefined"){module.exports=f()}else if(typeof define==="function"&&define.amd){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.TransactionManager = f()}})(function(){var define,module,exports;return (function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

function EventEmitter() {
  this._events = this._events || {};
  this._maxListeners = this._maxListeners || undefined;
}
module.exports = EventEmitter;

// Backwards-compat with node 0.10.x
EventEmitter.EventEmitter = EventEmitter;

EventEmitter.prototype._events = undefined;
EventEmitter.prototype._maxListeners = undefined;

// By default EventEmitters will print a warning if more than 10 listeners are
// added to it. This is a useful default which helps finding memory leaks.
EventEmitter.defaultMaxListeners = 10;

// Obviously not all Emitters should be limited to 10. This function allows
// that to be increased. Set to zero for unlimited.
EventEmitter.prototype.setMaxListeners = function(n) {
  if (!isNumber(n) || n < 0 || isNaN(n))
    throw TypeError('n must be a positive number');
  this._maxListeners = n;
  return this;
};

EventEmitter.prototype.emit = function(type) {
  var er, handler, len, args, i, listeners;

  if (!this._events)
    this._events = {};

  // If there is no 'error' event listener then throw.
  if (type === 'error') {
    if (!this._events.error ||
        (isObject(this._events.error) && !this._events.error.length)) {
      er = arguments[1];
      if (er instanceof Error) {
        throw er; // Unhandled 'error' event
      } else {
        // At least give some kind of context to the user
        var err = new Error('Uncaught, unspecified "error" event. (' + er + ')');
        err.context = er;
        throw err;
      }
    }
  }

  handler = this._events[type];

  if (isUndefined(handler))
    return false;

  if (isFunction(handler)) {
    switch (arguments.length) {
      // fast cases
      case 1:
        handler.call(this);
        break;
      case 2:
        handler.call(this, arguments[1]);
        break;
      case 3:
        handler.call(this, arguments[1], arguments[2]);
        break;
      // slower
      default:
        args = Array.prototype.slice.call(arguments, 1);
        handler.apply(this, args);
    }
  } else if (isObject(handler)) {
    args = Array.prototype.slice.call(arguments, 1);
    listeners = handler.slice();
    len = listeners.length;
    for (i = 0; i < len; i++)
      listeners[i].apply(this, args);
  }

  return true;
};

EventEmitter.prototype.addListener = function(type, listener) {
  var m;

  if (!isFunction(listener))
    throw TypeError('listener must be a function');

  if (!this._events)
    this._events = {};

  // To avoid recursion in the case that type === "newListener"! Before
  // adding it to the listeners, first emit "newListener".
  if (this._events.newListener)
    this.emit('newListener', type,
              isFunction(listener.listener) ?
              listener.listener : listener);

  if (!this._events[type])
    // Optimize the case of one listener. Don't need the extra array object.
    this._events[type] = listener;
  else if (isObject(this._events[type]))
    // If we've already got an array, just append.
    this._events[type].push(listener);
  else
    // Adding the second element, need to change to array.
    this._events[type] = [this._events[type], listener];

  // Check for listener leak
  if (isObject(this._events[type]) && !this._events[type].warned) {
    if (!isUndefined(this._maxListeners)) {
      m = this._maxListeners;
    } else {
      m = EventEmitter.defaultMaxListeners;
    }

    if (m && m > 0 && this._events[type].length > m) {
      this._events[type].warned = true;
      console.error('(node) warning: possible EventEmitter memory ' +
                    'leak detected. %d listeners added. ' +
                    'Use emitter.setMaxListeners() to increase limit.',
                    this._events[type].length);
      if (typeof console.trace === 'function') {
        // not supported in IE 10
        console.trace();
      }
    }
  }

  return this;
};

EventEmitter.prototype.on = EventEmitter.prototype.addListener;

EventEmitter.prototype.once = function(type, listener) {
  if (!isFunction(listener))
    throw TypeError('listener must be a function');

  var fired = false;

  function g() {
    this.removeListener(type, g);

    if (!fired) {
      fired = true;
      listener.apply(this, arguments);
    }
  }

  g.listener = listener;
  this.on(type, g);

  return this;
};

// emits a 'removeListener' event iff the listener was removed
EventEmitter.prototype.removeListener = function(type, listener) {
  var list, position, length, i;

  if (!isFunction(listener))
    throw TypeError('listener must be a function');

  if (!this._events || !this._events[type])
    return this;

  list = this._events[type];
  length = list.length;
  position = -1;

  if (list === listener ||
      (isFunction(list.listener) && list.listener === listener)) {
    delete this._events[type];
    if (this._events.removeListener)
      this.emit('removeListener', type, listener);

  } else if (isObject(list)) {
    for (i = length; i-- > 0;) {
      if (list[i] === listener ||
          (list[i].listener && list[i].listener === listener)) {
        position = i;
        break;
      }
    }

    if (position < 0)
      return this;

    if (list.length === 1) {
      list.length = 0;
      delete this._events[type];
    } else {
      list.splice(position, 1);
    }

    if (this._events.removeListener)
      this.emit('removeListener', type, listener);
  }

  return this;
};

EventEmitter.prototype.removeAllListeners = function(type) {
  var key, listeners;

  if (!this._events)
    return this;

  // not listening for removeListener, no need to emit
  if (!this._events.removeListener) {
    if (arguments.length === 0)
      this._events = {};
    else if (this._events[type])
      delete this._events[type];
    return this;
  }

  // emit removeListener for all listeners on all events
  if (arguments.length === 0) {
    for (key in this._events) {
      if (key === 'removeListener') continue;
      this.removeAllListeners(key);
    }
    this.removeAllListeners('removeListener');
    this._events = {};
    return this;
  }

  listeners = this._events[type];

  if (isFunction(listeners)) {
    this.removeListener(type, listeners);
  } else if (listeners) {
    // LIFO order
    while (listeners.length)
      this.removeListener(type, listeners[listeners.length - 1]);
  }
  delete this._events[type];

  return this;
};

EventEmitter.prototype.listeners = function(type) {
  var ret;
  if (!this._events || !this._events[type])
    ret = [];
  else if (isFunction(this._events[type]))
    ret = [this._events[type]];
  else
    ret = this._events[type].slice();
  return ret;
};

EventEmitter.prototype.listenerCount = function(type) {
  if (this._events) {
    var evlistener = this._events[type];

    if (isFunction(evlistener))
      return 1;
    else if (evlistener)
      return evlistener.length;
  }
  return 0;
};

EventEmitter.listenerCount = function(emitter, type) {
  return emitter.listenerCount(type);
};

function isFunction(arg) {
  return typeof arg === 'function';
}

function isNumber(arg) {
  return typeof arg === 'number';
}

function isObject(arg) {
  return typeof arg === 'object' && arg !== null;
}

function isUndefined(arg) {
  return arg === void 0;
}

},{}],2:[function(require,module,exports){
"use strict";
 const EventEmitter = require('events');
 
class Namespace extends EventEmitter
{
	constructor(namespace,tm)
	{
		super();
		this.namespace = namespace;
		this.tm = tm;
	}
	
	cmd(name,data) 
	{
		return this.tm.cmd(name,data,this.namespace);
	}
	
	event(name,data) 
	{
		return this.tm.event(name,data,this.namespace);
	}
	
	close()
	{
		return this.tm.namespaces.delete(this.namespace);
	}
};

class TransactionManager extends EventEmitter
{
	constructor(transport)
	{
		super();
		this.maxId = 0;
		this.namespaces = new Map();
		this.transactions = new Map();
		this.transport = transport;
		
		//Message event listener
		this.listener = (msg) => {
			//Process message
			var message = JSON.parse(msg.utf8Data || msg.data);

			//Check type
			switch(message.type)
			{
				case "cmd" :
					//Create command
					const cmd = {
						name		: message.name,
						data		: message.data,
						namespace	: message.namespace,
						accept		: (data) => {
							//Send response back
							transport.send(JSON.stringify ({
								type	 : "response",
								transId	 : message.transId,
								data	 : data
							}));
						},
						reject	: (data) => {
							//Send response back
							transport.send(JSON.stringify ({
								type	 : "error",
								transId	 : message.transId,
								data	 : data
							}));
						}
					};
					
					//If it has a namespace
					if (cmd.namespace)
					{
						//Get namespace
						const namespace = this.namespaces.get(cmd.namespace);
						//If we have it
						if (namespace)
							//trigger event only on namespace
							namespace.emit("cmd",cmd);
						else
							//Launch event on main event handler
							this.emit("cmd",cmd);
					} else {
						//Launch event on main event handler
						this.emit("cmd",cmd);
					}
					break;
				case "response":
				{
					//Get transaction
					const transaction = this.transactions.get(message.transId);
					if (!transaction)
						return;
					//delete transacetion
					this.transactions.delete(message.transId);
					//Accept
					transaction.resolve(message.data);
					break;
				}
				case "error":
				{
					//Get transaction
					const transaction = this.transactions.get(message.transId);
					if (!transaction)
						return;
					//delete transacetion
					this.transactions.delete(message.transId);
					//Reject
					transaction.reject(message.data);
					break;
				}
				case "event":
					//Create event
					const event = {
						name		: message.name,
						data		: message.data,
						namespace	: message.namespace,
					};
					//If it has a namespace
					if (event.namespace)
					{
						//Get namespace
						var namespace = this.namespaces.get(event.namespace);
						//If we have it
						if (namespace)
							//trigger event
							namespace.emit("event",event);
						else
							//Launch event on main event handler
							this.emit("event",event);
					} else {
						//Launch event on main event handler
						this.emit("event",event);
					}
					break;
			}
		};
		
		//Add it
		this.transport.addListener ? this.transport.addListener("message",this.listener) : this.transport.addEventListener("message",this.listener);
	}
	
	cmd(name,data,namespace) 
	{
		return new Promise((resolve,reject) => {
			//Check name is correct
			if (!name || name.length===0)
				throw new Error("Bad command name");

			//Create command
			const cmd = {
				type	: "cmd",
				transId	: this.maxId++,
				name	: name,
				data	: data
			};
			//Check namespace
			if (namespace)
				//Add it
				cmd.namespace = namespace;
			//Serialize
			const json = JSON.stringify(cmd);
			//Add callbacks
			cmd.resolve = resolve;
			cmd.reject  = reject;
			//Add to map
			this.transactions.set(cmd.transId,cmd);
			
			try {
				//Send json
				this.transport.send(json);
			} catch (e) {
				//delete transacetion
				this.transactions.delete(cmd.transId);
				//rethrow
				throw e;
			}
		});
	}
	
	event(name,data,namespace) 
	{
		//Check name is correct
		if (!name || name.length===0)
			throw new Error("Bad event name");
		
		//Create command
		const event = {
			type	: "event",
			name	: name,
			data	: data
		};
		//Check namespace
		if (namespace)
			//Add it
			event.namespace = namespace;
		//Serialize
		const json = JSON.stringify(event);
		//Send json
		return this.transport.send(json);
	}
	
	namespace(ns)
	{
		//Check if we already have them
		let namespace = this.namespaces.get(ns);
		//If already have it
		if (namespace) return namespace;
		//Create one instead
		namespace = new Namespace(ns,this);
		//Store it
		this.namespaces.set(ns, namespace);
		//ok
		return namespace;
		
	}
	
	close()
	{
		//Erase namespaces
		for (const ns of this.namespace.values())
			//terminate it
			ns.close();
		//remove lisnters
		this.transport.removeListener ? this.transport.removeListener("message",this.listener) : this.transport.removeEventListener("message",this.listener);
	}
};

module.exports = TransactionManager;
},{"events":1}]},{},[2])(2)
});