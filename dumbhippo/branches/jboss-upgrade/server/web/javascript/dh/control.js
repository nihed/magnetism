var dh = {}
dh.control = {}
dh.lang = {}

var dhBaseUrl = "http://fresnel.dumbhippo.com:8080"

///////////////////////// metalinguistic band-aids

var dh = {}

dh.lang.mixin = function(obj, props){
	var tobj = {};
	for(var x in props){
		if (x == "toString" || typeof tobj[x] == "undefined"){
			obj[x] = props[x];
		}
	}
	return obj;
}

dh.inherits = function(subclass, superclass){
	if(typeof superclass != 'function'){ 
		dh.raise("superclass: "+superclass+" borken");
	}
	subclass.prototype = new superclass();
	subclass.prototype.constructor = subclass;
	subclass.superclass = superclass.prototype;
}

var defineClass = function(childConstructor, parentConstructor, childProps) {
	if (!parentConstructor)
		parentConstructor = Object;
	dh.inherits(childConstructor, parentConstructor);
	dh.lang.mixin(childConstructor.prototype, childProps);
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
})

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
	},
	
	toString: function() {
		return "[Person (" + this._id + "," + this._name + ")]"
	}
})

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
	},
	
	toString: function() {
		return "[ChatMessage (" + this._entity + "," + this._message + ")]"
	}
})

dh.control.NONMEMBER = 0;
dh.control.PARTICIPANT = 1;
dh.control.VISITOR = 2;

dh.control.ChatRoom = function(control, id) {
	this._control = control;
	this._id = id;
	this._participantJoinCount = 0;
	this._visitorJoinCount = 0;
	this._desiredState = dh.control.NONMEMBER
	
	this._members = {}
}

defineClass(dh.control.ChatRoom, null, 
{
	getId : function() {
		return this._id;
	},
	
	onUserJoin : function(entity) {
	},
	
	onUserLeave : function(entity) {
	},
	
	onMessage : function(message) {
	},
	
	sendMessage : function(text) {
		this._control.sendChatMessage(this._id, text);
	},
	
	join : function(participant) {
		var newState = participant ? dh.control.PARTICIPANT : dh.control.VISITOR
		if (newState == this._desiredState)
			return
		if (this._desiredState != dh.control.NONMEMBER)
			this._control._leaveChatRoom(this._id)
		this._desiredState = newState
		this._control._joinChatRoom(this._id, participant);
	},
	
	leave : function(participant) {
		if (this._desiredState != dh.control.NONMEMBER)
			this._control._leaveChatRoom(this._id);
		this._control._desiredState = dh.control.NONMEMBER
	},
	
	_reconnect : function() {
		var state = this._desiredState;
		if (state != dh.control.NONMEMBER) {
			this._control._joinChatRoom(this._id, state == dh.control.PARTICIPANT);
		}
	},
	
	_disconnect : function() {
	},
	
	_desiredState : function() {
		if (this._participantJoinCount > 0)
			return dh.control.PARTICIPANT;
		else if (this._visitorJoinCount > 0)
			return dh.control.VISITOR;
		else
			return dh.control.NONMEMBER;
	},
	
	_addMessage : function(message) {
		// FIXME store the message in a list
		this.onMessage(message);
	},
	
	_onUserJoin : function(person) {
		this.onUserJoin(person)
	},
		
	_onUserLeave : function(person) {
		this.onUserLeave(person)
	}
})

///////////////////////// Control object

//// AbstractControl shared among all control implementations

dh.control.AbstractControl = function() {
	this._callbacks = { 
		onConnected : function() {},
		onDisconnected : function() {}
	}
	
	this._connected = false
	
	this._allEntities = {}
	this._allChatRooms = []
}

defineClass(dh.control.AbstractControl, null, 
{
	dh: dh,

	isConnected : function() {
		return this._connected
	},
	
	peekEntity : function(id) {
		return this._allEntities[id];
	},

	createChatRoom : function(id) {
		var room = new dh.control.ChatRoom(this, id);
		this._allChatRooms.push(room);
		
		return room;
	},

	_getOrCreatePerson : function(id) {
		var entity = this._allEntities[id]
		if (!entity) {
			entity = new dh.control.Person(id)
			this._allEntities[id] = entity
		}
		return entity
	},
	
	_doForRoom : function(id, f) {
		for (var i = 0; i < this._allChatRooms.length; i++) {
			if (this._allChatRooms[i].getId() == id) {
				f(this._allChatRooms[i])
			}
		}
	},
	
	_reconnect : function() {
		this._allEntities = {}		
		for (var i = 0; i < this._allChatRooms.length; i++) {
			this._allChatRooms[i]._reconnect()
		}
	},
	
	_disconnect : function() {
		for (var i = 0; i < allChatRooms.length; i++) {
			this._allChatRooms[i]._disconnect()
		}
	},
	
	_onUserJoin : function(chatId, userId) {
		var person = this._getOrCreatePerson(userId)
		this._doForRoom(chatId, function(room) {
			room._onUserJoin(person)
		})
	},
	
	_onUserLeave : function(chatId, userId) {
		var person = this._getOrCreatePerson(userId)
		this._doForRoom(chatId, function(room) {
			room._onUserLeave(person)		
		})
	},
	
	_onMessage : function(chatId, userId, text, timestamp, serial) {
		var message = new dh.control.ChatMessage(this._getOrCreatePerson(userId),
			text, timestamp, serial)
		this._doForRoom(chatId, function(room) {
			room._addMessage(message)
		})
	}, 
	
	_userInfo : function(userId, name, photoUrl, currentSong, currentArtist, musicPlaying) {
		var u = this._getOrCreatePerson(userId)
		u.setName(name)
		u.setPhotoUrl(photoUrl)
		u.setCurrentSong(currentSong)
		u.setCurrentArtist(currentArtist)
		u.setMusicPlaying(musicPlaying)
	}
})

//// WebOnlyControl may be just a dummy object that never
//// gets onConnected, or someday maybe it does something useful

dh.control.WebOnlyControl = function() {
	dh.control.AbstractControl.call(this)
}

defineClass(dh.control.WebOnlyControl, dh.control.AbstractControl, {
	frobate : function() {
	}
})

//// NativeControl wraps the XPCOM or ActiveX object
dh.control.NativeControl = function(nativeObject) {
	dh.control.AbstractControl.call(this);
	this._native = nativeObject;
	
	var me = this;
	
	// Note that this creates a refcount cycle through COM or XPCOM that
	// can't be collected, but since we don't want our object to go away
	// until the Javascript context for the page is deleted, that shouldn't
	// be a big problem
	this._native.setListener({
		onConnect: function() {
			me._reconnect();
        },
        onDisconnect: function() {
        	me._disconnect();
        },
        onUserJoin: function(chatId, userId) {
        	me._onUserJoin(chatId, userId);
        },
        onUserLeave: function(chatId, userId) {
        	me._onUserLeave(chatId, userId);
        },
        onMessage: function(chatId, userId, message, timestamp, serial) {
        	me._onMessage(chatId, userId, message, timestamp, serial);
        },
        userInfo: function(userId, name, smallPhotoUrl, arrangementName, artistName, musicPlaying) {
        	me._userInfo(userId, name, smallPhotoUrl, arrangementName, artistName, musicPlaying);
        },
        // The QueryInterface function is used only for Firefox, where the callback
        // is used as a XPCOM object for IE, we just need a javascript object
        QueryInterface: function(aIID) {
            if (!aIID.equals(Components.interfaces.hippoIServiceListener) &&
                !aIID.equals(Components.interfaces.nsISupports))
                throw new Components.results.NS_ERROR_NO_INTERFACE
            return this
        }})
        
    this._native.start(dhBaseUrl);
}

defineClass(dh.control.NativeControl, dh.control.AbstractControl, {

	_joinChatRoom : function(chatId, participant) {
		this._native.joinChatRoom(chatId, participant);
	},
	
	_leaveChatRoom : function(chatId) {
		this._native.leaveChatRoom(chatId);
	},

	sendChatMessage : function(chatId, text) {
		this._native.sendChatMessage(chatId, text);
	},
	
	showChatWindow : function(chatId) {
		this._native.showChatWindow(chatId);
	}
})


//// build our appropriate control object

dh.control.control = null;

try {
	var firefoxControl = new HippoService();
	dh.control.control = new dh.control.NativeControl(firefoxControl);
} catch (e) {
	try {
		var ieControl = new ActiveXObject("HippoControl");
		dh.control.control = new dh.control.NativeControl(ieControl);
	} catch (e) {
		dh.control.control = new dh.control.WebOnlyControl();
	}
}
