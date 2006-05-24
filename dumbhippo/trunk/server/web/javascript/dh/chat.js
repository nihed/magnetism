dojo.provide("dh.chat")

dh.chat.MESSAGE_FONT_STYLE = "normal" 
dh.chat.DESCRIPTION_MESSAGE_FONT_STYLE = "italic"  

dh.chat.Message = function(userId, photoUrl, name, text, timestamp, serial) {
	this.userId = userId 
	this.photoUrl = photoUrl
	this.name = name
	this.text = text
	this.timestamp = timestamp
	this.serial = serial
	
	this._shortLocaleTime = function(date) {
		return date.toLocaleTimeString().replace(/(\d:\d\d):\d\d/, "$1")
	}

	this.timeString = function() {
		var now = new Date()
		var nowTimestamp = now.getTime()
	    var date = new Date(this.timestamp)
    
		if (nowTimestamp - this.timestamp < 24 * 60 * 60 * 1000 && now.getDate() == date.getDate()) {
			return this._shortLocaleTime(date)
		} else if (nowTimestamp - this.timestamp < 7 * 24 * 60 * 60 * 1000) {
			var weekday = [ "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" ][date.getDay()]
			return weekday + ", " + this._shortLocaleTime(date)
		} else {
			return date.toDateString()
		}
	}
}

dh.chat.MessageList = function(insertCallback, removeCallback, limit) {
	this._limit = limit
	this._insertCallback = insertCallback
	this._removeCallback = removeCallback
	
	this._messages = []
	
	this.addMessage = function(message) {
		// Find the place to insert this message in the list
		var insertPos = this._messages.length
		for (var i = 0; i < this._messages.length; i++) {
			if (this._messages[i].serial == message.serial) // Already in the list
				return
			if (this._messages[i].serial > message.serial) {
				insertPos = i
				break
			}
		}
		
		// Shorten the current list of messages if necessary
		if (this._limit && this._messages.length == this._limit) {
			if (insertPos == 0)
				return
		
			var old = this._messages.shift()
			this._removeCallback(old)
			insertPos--
		}
		
		if (insertPos > 0 && this._messages[insertPos - 1].userId == message.userId)
			message.userFirst = false
		else
			message.userFirst = true
		
		// Insert the new message in the correct place
		this._messages.splice(insertPos, 0, message)
		var before = null
		if (insertPos < this._messages.length - 1)
			before = this._messages[insertPos + 1]
		this._insertCallback(message, before)		
		
		if (before) {
			// If the 'userFirst' user for the next message, we need
			// to remove and reinsert it so that the GUI can be updated
			var oldNextIsFirst = before.userFirst
			before.userFirst = before.userId != message.userId
			
			if (before.userFirst != oldNextIsFirst) {
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
	
	this.clear = function() {
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

dh.chat.User = function(userId, photoUrl, name) {
	this.userId = userId 
	this.photoUrl = photoUrl
	this.name = name
}

dh.chat.UserList = function(insertCallback, removeCallback) {
	this._insertCallback = insertCallback
	this._removeCallback = removeCallback
	
	this._users = []
	
	this.userJoin = function(user) {
		// Find the place to insert this user in the list
		var insertPos = this._users.length
		var sortName = user.name.toLowerCase()
		for (var i = 0; i < this._users.length; i++) {
			if (this._users[i].userId == user.userId) // Already in the list
				return
			if (this._users[i].name.toLowerCase() > sortName) {
				insertPos = i
				break
			}
		}
		
		// Insert the new user in the correct place
		this._users.splice(insertPos, 0, user)
		var before = null
		if (insertPos < this._users.length - 1)
			before = this._users[insertPos + 1]
		this._insertCallback(user, before)		
	}
	
	this.userLeave = function(userId) {
		for (var i = 0; i < this._users.length; i++) {
			if (this._users[i].userId == userId) {
				var old = this._users[i]
				this._users.splice(i, 1)
				this._removeCallback(old)
			}
		}
	}
	
	this.getUser = function(userId) {
		for (var i = 0; i < this._users.length; i++) {
			if (this._users[i].userId == userId) {
				return this._users[i]
			}
		}	
		return null
	 }
	
	this.clear = function() {
		while (this._users.length > 0) {
			var old = this._users.pop()
			this._removeCallback(old)
		}
	}
	
	this.numUsers = function() {
		return this._users.length
	}
}

dh.chat.getMessageFontStyle = function(message) {
    var messageFontStyle = this.MESSAGE_FONT_STYLE   
    // if message serial is -1, it is a post description that is displayed as the first message
    if (message.serial == -1) {
        messageFontStyle = this.DESCRIPTION_MESSAGE_FONT_STYLE
    }
    return messageFontStyle  
}
