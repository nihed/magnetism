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

var defineConstructor = function(childConstructor, parentConstructor, childProps) {
	childConstructor.prototype = childProps;

	if (parentConstructor) {
		// mixin doesn't overwrite the childProps if they exist, so overriding works
		dojo.lang.mixin(childConstructor.prototype, parentConstructor.prototype);	
	}
}

///////////////////////// auxiliary / data-model objects

dh.control.Entity = function(id) {
	this.id = id;
}

defineConstructor(dh.control.Entity, null, 
{
	name : "",
	photoUrl : null,
	
	onChanged : function() {
	},

	getId : function() {
		return this.id;
	},

	getName : function() {
		return this.name;
 	},

	setName : function(name) {
		this.name = name;
		this.onChanged();
	},

	getPhotoUrl : function() {
		return this.photoUrl;
	},
	
	setPhotoUrl : function(url) {
		this.photoUrl = url;
		this.onChanged();
	}
});

dh.control.Person = function(id) {
	dh.control.Entity.call(this, id);
}

defineConstructor(dh.control.Person, dh.control.Entity, 
{
	currentSong : null,
	currentArtist : null,
	musicPlaying : false,
		
	getCurrentSong : function() {
		return this.currentSong;
	},
	
	setCurrentSong : function(song) {
		this.currentSong = song;
		this.onChanged();
	},
	
	getCurrentArtist : function() {
		return this.currentArtist;
	},
	
	setCurrentArtist : function(artist) {
		this.currentArtist = artist;
		this.onChanged();
	},
	
	getMusicPlaying : function() {
		return this.musicPlaying;
	},
	
	setMusicPlaying : function(playing) {
		this.musicPlaying = playing;
		this.onChanged();
	}
});

dh.control.ChatMessage = function(entity, message, timestamp, serial) {
	this.entity = entity;
	this.message = message;
	this.timestamp = timestamp;
	this.serial = serial;
}

defineConstructor(dh.control.ChatMessage, null,
{
	getEntity : function() {
		return this.entity;
	},
	
	getMessage : function() {
		return this.message;
	},
	
	getTimestamp : function() {
		return this.timestamp;
	},
	
	getSerial : function() {
		return this.serial;
	}
});

dh.control.ChatRoom = function(control, id) {
	this.control = control;
	this.id = id;
}

defineConstructor(dh.control.ChatRoom, null, 
{
	getId : function() {
		return this.id;
	},
	
	onUserJoined : function(entity) {
	}
	
	onUserLeft : function(entity) {
	}
	
	onMessage : function(message) {
	}
	
	sendMessage : function(text) {
		this.control.sendChatMessage(this.id, text);
	}
	
	join : function(participant) {
		this.control.joinChatRoom(this.id, participant);
	}
	
	leave : function() {
		this.control.leaveChatRoom(this.id);
	},
	
	addMessage : function(message) {
		// FIXME store the message in a list
		this.onMessage(message);
	}
		
});

///////////////////////// Control object

//// AbstractControl shared among all control implementations

dh.control.AbstractControl = function() {
}

defineConstructor(dh.control.AbstractControl, null, 
{
	callbacks : { 
		onConnected : function() {},
		onDisconnected : function() {}
	},

	// control users outside this file are supposed to set these
	setCallbacks : function(callbacks) {
		this.callbacks = callbacks;
	},

	connected : false,

	onConnected : function() {
		this.connected = true;
		this.callbacks.onConnected();
	},

	onDisconnected : function() {
		this.connected = false;
		// dump all our data
		this.allEntities = {};
		this.allChatRooms = {};
		// emit signal
		this.callbacks.onDisconnected();
	},

	isConnected : function() {
		return connected;
	}
	
	allEntities : {},
	
	getOrCreatePerson : function(id) {
		var entity = this.allEntities[id];
		if (!entity) {
			entity = new dh.control.Person(id);
			this.allEntities[id] = entity;
		}
		return entity;
	},
	
	peekEntity : function(id) {
		return this.allEntities[id];
	},
	
	allChatRooms : {},
	
	getOrCreateChatRoom : function(id) {
		var room = this.allChatRooms[id];
		if (!room) {
			room = new dh.control.ChatRoom(this, id);
			this.allChatRooms[id] = room;
		}
		return room;	
	},
	
	peekChatRoom : function(id) {
		return this.allChatRooms[id];
	},
	
	onUserJoined : function(chatId, userId) {
		var room = this.peekChatRoom(chatId);
		if (!room)
			return;
		room.onUserJoined(this.getOrCreatePerson(userId));		
	},
	
	onUserLeft : function(chatId, userId) {
		var room = this.peekChatRoom(chatId);
		if (!room)
			return;	
		room.onUserLeft(this.getOrCreatePerson(userId));					
	},
	
	onMessage : function(chatId, userId, text, timestamp, serial) {
		var room = this.peekChatRoom(chatId);
		if (!room)
			return;
		var message = new dh.control.ChatMessage(this.getOrCreatePerson(userId),
			text, timestamp, serial);
		room.addMessage(message);
	}, 
	
	onUserInfo : function(userId, name, photoUrl, currentSong, currentArtist, musicPlaying) {
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
}

defineConstructor(dh.control.WebOnlyControl, dh.control.AbstractControl, {
	frobate : function() {
	}
});

//// NativeControl wraps the XPCOM or ActiveX object
dh.control.NativeControl = function(native) {
	this.native = native;
}

defineConstructor(dh.control.NativeControl, dh.control.AbstractControl, {

	joinChatRoom : function(chatId, participant) {
		native.joinChatRoom(chatId, participant);
		return this.getOrCreateChatRoom(chatId);
	}
	
	leaveChatRoom : function(chatId) {
		native.leaveChatRoom(chatId);
	}

	sendChatMessage : function(chatId, text) {
		native.sendChatMessage(chatId, text);
	},
	
	showChatWindow : function(chatId) {
		native.showChatWindow(chatId);
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
