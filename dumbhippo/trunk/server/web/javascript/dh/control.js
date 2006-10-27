dojo.provide("dh.control")

///////////////////////// metalinguistic band-aids

dh.lang = {}

dh.lang.mixin = function(obj, props){
	var tobj = {};
	for(var x in props){
		if (x == "toString" || typeof tobj[x] == "undefined"){
			obj[x] = props[x];
		}
	}
	return obj;
}

var defineClass = function(childConstructor, parentConstructor, childProps) {
	if (!parentConstructor)
		parentConstructor = Object;
	dojo.inherits(childConstructor, parentConstructor);
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
	getId : function() {
		return this._id;
	},

	getName : function() {
		return this._name;
 	},

	setName : function(name) {
		this._name = name;
	},

	getPhotoUrl : function() {
		return this._photoUrl;
	},
	
	setPhotoUrl : function(url) {
		this._photoUrl = url;
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
	},
	
	getCurrentArtist : function() {
		return this._currentArtist;
	},
	
	setCurrentArtist : function(artist) {
		this._currentArtist = artist;
	},
	
	getMusicPlaying : function() {
		return this._musicPlaying;
	},
	
	setMusicPlaying : function(playing) {
		this._musicPlaying = playing;
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
	
	_shortLocaleTime : function(date) {
		return date.toLocaleTimeString().replace(/(\d:\d\d):\d\d/, "$1")
	},

	timeString : function() {
		var now = new Date();
		var nowTimestamp = now.getTime();
	    var date = new Date(this._timestamp);
    
		if (nowTimestamp - this._timestamp < 24 * 60 * 60 * 1000 && now.getDate() == date.getDate()) {
			return this._shortLocaleTime(date);
		} else if (nowTimestamp - this._timestamp < 7 * 24 * 60 * 60 * 1000) {
			var weekday = [ "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" ][date.getDay()];
			return weekday + ", " + this._shortLocaleTime(date);
		} else {
			return date.toDateString();
		}
	},	
	
	toString: function() {
		return "[ChatMessage (" + this._entity + "," + this._message + "," + this.timeString() + ")]";
	}
})

dh.control.NONMEMBER = 0;
dh.control.PARTICIPANT = 1;
dh.control.VISITOR = 2;

dh.control.ChatUser = function(user, participant) {
	this._user = user;
	this._participant = participant;
}

defineClass(dh.control.ChatUser, null,
{
	getUser : function() {
		return this._user;
	},
	
	getParticipant : function() {
		return this._participant;
	}
})

dh.control.ChatRoom = function(control, id) {
	this._id = id;
	this._participantJoinCount = 0;
	this._visitorJoinCount = 0;
	
	this._users = {}
	this._messages = {}
}

defineClass(dh.control.ChatRoom, null, 
{
	getId : function() {
		return this._id;
	},
	
	onUserJoin : function(entity, participant) {
	},
	
	onUserLeave : function(entity) {
	},
	
	onMessage : function(message) {
	},
	
	onReconnect : function() {
	},
	
	sendMessage : function(text) {
		dh.control.control.sendChatMessage(this._id, text);
	},
	
	join : function(participant) {
		var oldState = this._desiredState();
		if (participant)
			this._participantJoinCount++;
		else
			this._visitorJoinCount++;
		var newState = this._desiredState();
		if (newState != oldState) {
			if (oldState != dh.control.NONMEMBER)
				dh.control.control._leaveChatRoom(this._id);
			dh.control.control._joinChatRoom(this._id, newState == dh.control.PARTICIPANT);
		}
	},
	
	leave : function(participant) {
		var oldState = this._desiredState();
		if (participant && this._participantJoinCount > 0)
			this._participantJoinCount--;
		else if (this._visitorJoinCount > 0)
			this._visitorJoinCount--;
		var newState = this._desiredState();
		if (newState != oldState) {
			dh.control.control._leaveChatRoom(this._id);
			if (newState == dh.control.NONMEMBER) {
				// FIXME: remove existing users
			} else {
				dh.control.control._joinChatRoom(this._id, newState == dh.control.PARTICIPANT);
			}
		}
	},
	
	_reconnect : function() {
		this._users = {};
		this._messages = {};
		
		var state = this._desiredState();
		if (state != dh.control.NONMEMBER) {
			this.onReconnect();
		
			dh.control.control._joinChatRoom(this._id, state == dh.control.PARTICIPANT);
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
		this._messages[message.getSerial()] = message;
		this.onMessage(message);
	},
	
	_onUserJoin : function(person, participant) {
		var chatUser = this._users[person.getId()]
		if (chatUser) {
			if (chatUser.getParticipant() == participant)
				return;
			this._onUserLeave(person);
		}
		this._users[person.getId()] = new dh.control.ChatUser(person, participant);
		this.onUserJoin(person, participant);
	},
		
	_onUserLeave : function(person) {
		delete this._users[person.getId()];
		this.onUserLeave(person);
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
	this._allChatRooms = {}
}

defineClass(dh.control.AbstractControl, null, 
{
	// Connection point for notification of a change to a user; it would
	// be more pleasant to be able to connect to the user itself, but 
	// creating lots of small closures is memory intensive and prone to
	// creation of uncollectable cycles
	onUserChange : function(user) {
	},

	isConnected : function() {
		return this._connected;
	},
	
	peekEntity : function(id) {
		return this._allEntities[id];
	},

	getOrCreateChatRoom : function(id) {
		var room = this._allChatRooms[id]
		if (!room) {
			room = new dh.control.ChatRoom(this, id);
			this._allChatRooms[id] = room;
		}
		
		return room;
	},

	_getOrCreatePerson : function(id) {
		var entity = this._allEntities[id]
		if (!entity) {
			entity = new dh.control.Person(id);
			this._allEntities[id] = entity;
		}
		return entity
	},
	
	_doForRoom : function(id, f) {
		if (this._allChatRooms[id]) {
			f(this._allChatRooms[id]);
		}
	},
	
	_reconnect : function() {
		this._allEntities = {}		
		for (var id in this._allChatRooms) {
			this._allChatRooms[id]._reconnect();
		}
	},
	
	_disconnect : function() {
		for (var id in this._allChatRooms) {
			this._allChatRooms[id]._disconnect();
		}
	},
	
	_onUserJoin : function(chatId, userId, participant) {
		if (this._allChatRooms[chatId]) {
			var person = this._getOrCreatePerson(userId);
			this._allChatRooms[chatId]._onUserJoin(person, participant);
		}
	},
	
	_onUserLeave : function(chatId, userId) {
		if (this._allChatRooms[chatId]) {
			var person = this._getOrCreatePerson(userId);
			this._allChatRooms[chatId]._onUserLeave(person)	;	
		}
	},
	
	_onMessage : function(chatId, userId, text, timestamp, serial) {
		if (this._allChatRooms[chatId]) {	
			var message = new dh.control.ChatMessage(this._getOrCreatePerson(userId),
											 		 text, timestamp, serial);
			this._allChatRooms[chatId]._addMessage(message);
		}
	}, 
	
	_userInfo : function(userId, name, photoUrl, currentSong, currentArtist, musicPlaying) {
		var u = this._getOrCreatePerson(userId);
		u.setName(name);
		u.setPhotoUrl(photoUrl);
		u.setCurrentSong(currentSong);
		u.setCurrentArtist(currentArtist);
		u.setMusicPlaying(musicPlaying);
		this.onUserChange(u)
	}
});

//// WebOnlyControl may be just a dummy object that never
//// gets onConnected, or someday maybe it does something useful

dh.control.WebOnlyControl = function() {
	dh.control.AbstractControl.call(this)
}

defineClass(dh.control.WebOnlyControl, dh.control.AbstractControl, {
	sendChatMessage : function(chatId, text) {
	},
	
	showChatWindow : function(chatId) {
	},

	haveLiveChat : function(chatId) {
		return false
	}
});

//// NativeControl wraps the XPCOM or ActiveX object
dh.control.NativeControl = function(nativeObject) {
	dh.control.AbstractControl.call(this);
	this._native = nativeObject;
	
	this._native.setListener(new dh.control.NativeControlListener())
        
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
	},
	
	haveLiveChat : function(chatId) {
		return true;
	}
});

// You'd expect this object to take a NativeControl as a constructor argument
// and call the callbacks on that, rather than on dh.control.control,
// but that would create a refcount cycle through XPCOM (the
// XPCOM Object => NativeControlListenre => NativeControl => XPCOM Object)
// and things would never get freed. So we just hardcode a call to the global 
// singleton dh.control.control instead.
dh.control.NativeControlListener = function() {
}

defineClass(dh.control.NativeControlListener, null, {
	onConnect: function() {
		dh.control.control._reconnect();
    },
    onDisconnect: function() {
    	dh.control.control._disconnect();
    },
    onUserJoin: function(chatId, userId, participant) {
    	dh.control.control._onUserJoin(chatId, userId, participant);
    },
    onUserLeave: function(chatId, userId) {
    	dh.control.control._onUserLeave(chatId, userId);
    },
    onMessage: function(chatId, userId, message, timestamp, serial) {
    	dh.control.control._onMessage(chatId, userId, message, timestamp, serial);
    },
    userInfo: function(userId, name, smallPhotoUrl, arrangementName, artistName, musicPlaying) {
    	dh.control.control._userInfo(userId, name, smallPhotoUrl, arrangementName, artistName, musicPlaying);
    },
    // The QueryInterface function is used only for Firefox, where the callback
    // is used as a XPCOM object for IE, we just need a javascript object
    QueryInterface: function(aIID) {
        if (!aIID.equals(Components.interfaces.hippoIControlListener) &&
            !aIID.equals(Components.interfaces.nsISupports))
            throw new Components.results.NS_ERROR_NO_INTERFACE
        return this
    }
});

//// build our appropriate control object
dh.control.control = null; // init variable to null on load

dh.control.createControl = function() {
	// note that we may be called multiple times, all after
	// the first should no-op

	if (!dh.control.control) {
		var firefoxControl
		try { 
			firefoxControl  = new HippoControl(); 
		} catch (e) {}
	
		if (firefoxControl)
			dh.control.control = new dh.control.NativeControl(firefoxControl);
	} 
	
	if (!dh.control.control) {
		var ieControl
		try {
			ieControl = new ActiveXObject("Hippo.Control");
		} catch (e) {}
		
		if (ieControl)
			dh.control.control = new dh.control.NativeControl(ieControl);
	}
	
	if (!dh.control.control) {
		dh.control.control = new dh.control.WebOnlyControl();
	}
}

//
// We want to make sure that our control isn't leaked even if the
// web browser isn't cleaned up properly, since that can cause a
// user to appear in a chatroom after they are gone. So on teardown,
// we clear the references to the control forcibly
//
if (dojo.render.html.ie) {
	dh.control.oldOnUnload = window.onunload
	window.onunload = function() {
		dh.control.control._native = null;
		dh.control.control = null;
		if (dh.control.oldOnUnload) {
			dh.control.oldOnUnload()
		}
	}
}
