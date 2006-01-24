dojo.provide("dh.framer")

dojo.require("dh.chat")
dojo.require("dh.util")
dojo.require("dojo.event")

dh.framer._selfId = null
dh.framer._messageList = new dh.chat.MessageList(
	function(message, before) { dh.framer._addMessage(message, before) },
	function(message) { dh.framer._removeMessage(message) },
	3)
dh.framer._participantList = new dh.chat.UserList(
	function(user, before) { dh.framer._addUser(user, before, true) },
	function(user) { dh.framer._removeUser(user, true) })
dh.framer._visitorList = new dh.chat.UserList(
	function(user, before) { dh.framer._addUser(user, before, false) },
	function(user) { dh.framer._removeUser(user, false) })

// Add a user to the list of current users
dh.framer.onUserJoin = function(userId, version, name, participant) {
	var user = new dh.chat.User(userId, version, name)
	if (participant) {
		this._visitorList.userLeave(userId)
		this._participantList.userJoin(user)
	} else {
		this._participantList.userLeave(userId)
		this._visitorList.userJoin(user)
	}
}

// Remove the user from the list of current users
dh.framer.onUserLeave = function(userId) {
	this._visitorList.userLeave(userId)
	this._participantList.userLeave(userId)
}

// Add a message to the message area
dh.framer.onMessage = function(userId, version, name, text, timestamp, serial) {
	var message = new dh.chat.Message(userId, version, name, text, timestamp, serial)
	this._messageList.addMessage(message)
}

// Clear all messages and users (called on reconnect)
dh.framer.onReconnect = function() {
	this._participantList.clear()
	this._visitorList.clear()
	this._messageList.clear()
}

dh.framer._addMessage = function(message, before) {
	message.div = document.createElement("div")
    message.div.className = "dh-chat-preview-message"
    var text = message.name + ": " + message.text + "  (" + message.timeString() + ")"
	message.div.appendChild(document.createTextNode(text))
	
    var previewArea = document.getElementById("dhChatPreview")
	previewArea.insertBefore(message.div, before ? before.div : null)
}

dh.framer._removeMessage = function(message) {
    var previewArea = document.getElementById("dhChatPreview")
	previewArea.removeChild(message.div)
}

dh.framer._getUserListSpan = function(participant) {
	if (participant)
	    return document.getElementById("dhChatParticipantList")
    else
	    return document.getElementById("dhChatVisitorList")
}

dh.framer._addUser = function(user, before, participant) {
	var userList = this._getUserListSpan(participant)
    
    user.span = document.createElement("span")
    user.span.appendChild(document.createTextNode(user.name))
    
	userList.insertBefore(user.span, before)
	if (user.span.nextSibling)
		userList.insertBefore(document.createTextNode(", "), user.span.nextSibling)
	else if (user.span.previousSibling)
		userList.insertBefore(document.createTextNode(", "), user.span)
}

dh.framer._removeUser = function(user, participant) {
	var userList = this._getUserListSpan(participant)
    
    if (user.span.nextSibling)
	    userList.removeChild(user.span.nextSibling)
    userList.removeChild(user.span)
}

dh.framer.setSelfId = function(id) {
	this._selfId = id
}

dh.framer.init = function() {
}
