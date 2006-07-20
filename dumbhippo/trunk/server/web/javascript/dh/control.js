var dh = {}
dh.control = {}

///////////////////////// metalinguistic band-aids

var dojo = {}
dojo.lang = {}

dojo.lang.mixin = function(obj, props){
	var tobj = {};
	for(var x in props){
		if(typeof tobj[x] == "undefined"){
			obj[x] = props[x];
		}
	}
	return obj;
}

dojo.inherits = function(subclass, superclass){
	if(typeof superclass != 'function'){ 
		dojo.raise("superclass: "+superclass+" borken");
	}
	subclass.prototype = new superclass();
	subclass.prototype.constructor = subclass;
	subclass.superclass = superclass.prototype;
}

var defineClass = function(childConstructor, parentConstructor, childProps) {
	if (!parentConstructor)
		parentConstructor = Object;
	dojo.inherits(childConstructor, parentConstructor);
	dojo.lang.mixin(childConstructor.prototype, childProps);
}

///////////////////////// auxiliary / data-model objects

dh.control.Entity = function(id) {
	this._id = id;
	this._name = "";
	this._photoUrl = null;
	
}

defineClass(dh.control.Entity, null, 
{
	onChanged : function() {
	},

	getId : function() {
		return this._id;
	},

	getName : function() {
		return this._name;
 	},

	setName : function(name) {
		this._name = name;
		this.onChanged();
	},

	getPhotoUrl : function() {
		return this._photoUrl;
	},
	
	setPhotoUrl : function(url) {
		this._photoUrl = url;
		this.onChanged();
	}
});

dh.control.Person = function(id) {
	dh.control.Entity.call(this, id);
	this._currentSong = null;
	this._currentArtist = null;
	this._musicPlaying = false;
}

defineClass(dh.control.Person, dh.control.Entity, 
{		
	getCurrentSong : function() {
		return this._currentSong;
	},
	
	setCurrentSong : function(song) {
		this._currentSong = song;
		this.onChanged();
	},
	
	getCurrentArtist : function() {
		return this._currentArtist;
	},
	
	setCurrentArtist : function(artist) {
		this._currentArtist = artist;
		this.onChanged();
	},
	
	getMusicPlaying : function() {
		return this._musicPlaying;
	},
	
	setMusicPlaying : function(playing) {
		this._musicPlaying = playing;
		this.onChanged();
	}
});

dh.control.ChatMessage = function(entity, message, timestamp, serial) {
	this._entity = entity;
	this._message = message;
	this._timestamp = timestamp;
	this._serial = serial;
}

defineClass(dh.control.ChatMessage, null,
{
	getEntity : function() {
		return this._entity;
	},
	
	getMessage : function() {
		return this._message;
	},
	
	getTimestamp : function() {
		return this._timestamp;
	},
	
	getSerial : function() {
		return this._serial;
	}
});

dh.control.ChatRoom = function(control, id) {
	this._control = control;
	this._id = id;
}

defineClass(dh.control.ChatRoom, null, 
{
	getId : function() {
		return this._id;
	},
	
	onUserJoined : function(entity) {
	}
	
	onUserLeft : function(entity) {
	}
	
	onMessage : function(message) {
	}
	
	sendMessage : function(text) {
		this._control.sendChatMessage(this._id, text);
	}
	
	join : function(participant) {
		this._control.joinChatRoom(this._id, participant);
	}
	
	leave : function() {
		this._control.leaveChatRoom(this._id);
	},
	
	_addMessage : function(message) {
		// FIXME store the message in a list
		this.onMessage(message);
	}
		
});

///////////////////////// Control object

//// AbstractControl shared among all control implementations

dh.control.AbstractControl = function() {
	this._callbacks = { 
		onConnected : function() {},
		onDisconnected : function() {}
	};
	
	this._connected = false;
	
	this._allEntities = {};
	this._allChatRooms = {};
}

defineClass(dh.control.AbstractControl, null, 
{
	// control users outside this file are supposed to set these
	setCallbacks : function(callbacks) {
		this._callbacks = callbacks;
	},

	onConnected : function() {
		this._connected = true;
		this._callbacks.onConnected();
	},

	onDisconnected : function() {
		this._connected = false;
		// dump all our data
		this._allEntities = {};
		this._allChatRooms = {};
		// emit signal
		this._callbacks.onDisconnected();
	},

	isConnected : function() {
		return this._connected;
	},
	
	getOrCreatePerson : function(id) {
		var entity = this._allEntities[id];
		if (!entity) {
			entity = new dh.control.Person(id);
			this._allEntities[id] = entity;
		}
		return entity;
	},
	
	peekEntity : function(id) {
		return this._allEntities[id];
	},
	
	getOrCreateChatRoom : function(id) {
		var room = this._allChatRooms[id];
		if (!room) {
			room = new dh.control.ChatRoom(this, id);
			this._allChatRooms[id] = room;
		}
		return room;	
	},
	
	peekChatRoom : function(id) {
		return this._allChatRooms[id];
	},
	
	_onUserJoined : function(chatId, userId) {
		var room = this.peekChatRoom(chatId);
		if (!room)
			return;
		room.onUserJoined(this.getOrCreatePerson(userId));		
	},
	
	_onUserLeft : function(chatId, userId) {
		var room = this.peekChatRoom(chatId);
		if (!room)
			return;	
		room.onUserLeft(this.getOrCreatePerson(userId));					
	},
	
	_onMessage : function(chatId, userId, text, timestamp, serial) {
		var room = this.peekChatRoom(chatId);
		if (!room)
			return;
		var message = new dh.control.ChatMessage(this.getOrCreatePerson(userId),
			text, timestamp, serial);
		room._addMessage(message);
	}, 
	
	_onUserInfo : function(userId, name, photoUrl, currentSong, currentArtist, musicPlaying) {
		var u = this.getOrCreatePerson(userId);
		u.setName(name);
		u.setPhotoUrl(photoUrl);
		u.setCurrentSong(currentSong);
		u.setCurrentArtist(currentArtist);
		u.setMusicPlaying(musicPlaying);
	}

});

//// WebOnlyControl may be just a dummy object that never
//// gets onConnected, or someday maybe it does something useful

dh.control.WebOnlyControl = function() {
	dh.control.AbstractControl.call(this);
}

defineClass(dh.control.WebOnlyControl, dh.control.AbstractControl, {
	frobate : function() {
	}
});

//// NativeControl wraps the XPCOM or ActiveX object
dh.control.NativeControl = function(native) {
	dh.control.AbstractControl.call(this);
	this._native = native;
}

defineClass(dh.control.NativeControl, dh.control.AbstractControl, {

	joinChatRoom : function(chatId, participant) {
		this._native.joinChatRoom(chatId, participant);
		return this.getOrCreateChatRoom(chatId);
	}
	
	leaveChatRoom : function(chatId) {
		this._native.leaveChatRoom(chatId);
	}

	sendChatMessage : function(chatId, text) {
		this._native.sendChatMessage(chatId, text);
	},
	
	showChatWindow : function(chatId) {
		this._native.showChatWindow(chatId);
	}
});


//// build our appropriate control object

dh.control.control = null;

try {
	var firefoxControl = new HippoControl();
	dh.control.control = new dh.control.NativeControl(firefoxControl);
} catch (e) {
	try {
		var ieControl = new ActiveXObject("HippoControl");
		dh.control.control = new dh.control.NativeControl(ieControl);
	} catch (e) {
		dh.control.control = new dh.control.WebOnlyControl();
	}
}
