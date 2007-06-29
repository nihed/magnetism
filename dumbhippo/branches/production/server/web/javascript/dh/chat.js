dojo.provide("dh.chat");
dojo.require("dh.event");

dh.chat.MESSAGE_FONT_STYLE = "normal" 
dh.chat.DESCRIPTION_MESSAGE_FONT_STYLE = "italic"  

dh.chat.MessageList = function(chatRoom, insertCallback, removeCallback, limit, newestOnTop) {
	this._chatRoom = chatRoom
	this._limit = limit
	this._newestOnTop = newestOnTop
	this._insertCallback = insertCallback
	this._removeCallback = removeCallback
	
	this._messages = []
	
	dojo.event.connect(chatRoom, "onMessage", this, "_onMessage")
	dojo.event.connect(chatRoom, "onReconnect", this, "_onReconnect")

	this._onMessage = function(message) {
		// Find the place to insert this message in the list
	    var insertPos 
		if (this._newestOnTop)
		    insertPos = 0
		else
		    insertPos = this._messages.length
	
		for (var i = 0; i < this._messages.length; i++) {
		    if (this._newestOnTop) {
		        j = this._messages.length - i - 1;
			    if (this._messages[j].getSerial() == message.getSerial()) // Already in the list
				    return
			    if (this._messages[j].getSerial() > message.getSerial()) {
				    insertPos = j + 1
				    break
			    }			
			} else {
			    if (this._messages[i].getSerial() == message.getSerial()) // Already in the list
				    return
			    if (this._messages[i].getSerial() > message.getSerial()) {
				    insertPos = i
				    break
			    }
			}
		}
		
		// Shorten the current list of messages if necessary
		if (this._limit && this._messages.length == this._limit) {
		    // return if we have no space for the message we were considering adding 
			if ((insertPos == 0 && !this._newestOnTop) || (insertPos == this._messages.length && this._newestOnTop))
				return
		
		    var old
		    if (this._newestOnTop) {
		        old =  this._messages.pop()
		    } else {        
			    old = this._messages.shift()
			    insertPos--
			}
			this._removeCallback(old)
		}
		
		if (insertPos > 0 && !this._newestOnTop) {
		    var previous = this._messages[insertPos - 1];
		    message.userFirst = message.getEntity() != previous.getEntity();
			message.sentimentFirst = message.userFirst || (message.getSentiment() != previous.getSentiment());
		} else if (insertPos < this._messages.length - 1 && this._newestOnTop) {
		    var previous = this._messages[insertPos + 1];
		    message.userFirst = message.getEntity() != previous.getEntity();
			message.sentimentFirst = message.userFirst || (message.getSentiment() != previous.getSentiment());			
		} else {
			message.userFirst = true;
			message.sentimentFirst = true;
		}
			
		// Insert the new message in the correct place
		this._messages.splice(insertPos, 0, message)
		var before = null
		if (insertPos < this._messages.length - 1)
			before = this._messages[insertPos + 1]
		this._insertCallback(message, before)		
		
		// we don't care about readjusting userFirst and sentimentFirst
		// in the newestOnTop mode, but if we did, we'd need to do it here
		// for a message after which we've inserted the new message
		if (before && !this._newestOnTop) {
			// We might have to reinsert the next entry if 
			// "userFirst" or "sentimentFirst" for it changed
			var oldNextIsUserFirst = before.userFirst;
			var oldNextIsSentimentFirst = before.sentimentFirst;
			before.userFirst = before.getEntity() != message.getEntity();
			before.sentimentFirst = before.userFirst || before.getSentiment() != message.getSentiment();
			
			if (before.userFirst != oldNextIsUserFirst || before.sentimentFirst != oldNextIsSentimentFirst) {
				var nextBefore
				if (insertPos < this._messages.length - 2)
					nextBefore = this._messages[insertPos + 2]

				this._messages.splice(insertPos + 1, 1)
				this._removeCallback(before)
				this._messages.splice(insertPos + 1, 0, before)
				this._insertCallback(before, nextBefore)
			}
		}
	}
	
	this._onReconnect = function() {
		while (this._messages.length > 0) {
			var old = this._messages.pop()
			this._removeCallback(old)
		}
	}
	
	this.foreachMessage = function(callback) {
	    for (var i = 0; i < this._messages.length; i++) {
	        var before = null
		    if (i < this._messages.length - 1)
			    before = this._messages[i + 1]
	        
			callback(this._messages[i], before)
		}
	}
	
	this.numMessages = function() {
		return this._messages.length
	}
}

dh.chat.UserList = function(chatRoom, insertCallback, removeCallback, updateCallback, userFilter) {
	this._chatRoom = chatRoom
	this._insertCallback = insertCallback
	this._removeCallback = removeCallback
	this._updateCallback = updateCallback
	this._userFilter = userFilter
	
	this._users = []
	this._userMap = {}

	dojo.event.connect(chatRoom, "onUserJoin", this, "_onUserJoin")
	dojo.event.connect(chatRoom, "onUserLeave", this, "_onUserLeave")	
	dojo.event.connect(chatRoom, "onReconnect", this, "_onReconnect")	
	dojo.event.connect(dh.control.control, "onUserChange", this, "_onUserChange")
	
	this._onUserJoin = function(user, participant) {
		if (this._userFilter && !this._userFilter(user, participant))
			return
			
		// Find the place to insert this user in the list
		var insertPos = this._users.length
		var sortName = user.getName().toLowerCase()
		var userId = user.getId()
		for (var i = 0; i < this._users.length; i++) {
			if (this._users[i].getId() == userId) // Already in the list
				return
			if (this._users[i].getName().toLowerCase() > sortName) {
				insertPos = i
				break
			}
		}
		
		this._userMap[user.getId()] = user;
		
		// Insert the new user in the correct place
		this._users.splice(insertPos, 0, user)
		var before = null
		if (insertPos < this._users.length - 1)
			before = this._users[insertPos + 1]
		this._insertCallback(user, before, participant)		
	}
	
	this._onUserLeave = function(user) {
		delete this._userMap[user.getId()];
		
		for (var i = 0; i < this._users.length; i++) {
			if (this._users[i] == user) {
				var old = this._users[i]
				this._users.splice(i, 1)
				this._removeCallback(old)
			}
		}
	}
	
	this._onReconnect = function() {
		while (this._users.length > 0) {
			var old = this._users.pop()
			delete this._userMap[old.getId()];
			this._removeCallback(old)
		}
	}
	
	this._onUserChange = function(user) {
		if (this._userMap[user.getId()]) {
			this._updateCallback(user)
		}
	}
	
	this.numUsers = function() {
		return this._users.length
	}
}

dh.chat.getMessageFontStyle = function(message) {
    var messageFontStyle = this.MESSAGE_FONT_STYLE   
    // if message serial is -1, it is a post description that is displayed as the first message
    if (message.getSerial() == -1) {
        messageFontStyle = this.DESCRIPTION_MESSAGE_FONT_STYLE
    }
    return messageFontStyle  
}
