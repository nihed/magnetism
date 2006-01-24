dojo.provide("dh.framer")

dojo.require("dh.chat")
dojo.require("dh.util")
dojo.require("dojo.event")

dh.framer._selfId = null
dh.framer._messageList = new dh.chat.MessageList(
	function(message, before) { dh.framer._addMessage(message, before) },
	function(message) { dh.framer._removeMessage(message) },
	3)
dh.framer._userList = new dh.chat.UserList(
	function(user, before) { dh.framer._addUser(user, before) },
	function(user) { dh.framer._removeUser(user) })

dh.framer._personDivs = {}
    
// Add a user to the list of current users
dh.framer.onUserJoin = function(userId, version, name) {
	var user = new dh.chat.User(userId, version, name)
	this._userList.userJoin(user)
}

// Remove the user from the list of current users
dh.framer.onUserLeave = function(userId) {
	this._userList.userLeave(userId)
}

// Add a message to the message area
dh.framer.onMessage = function(userId, version, name, text, timestamp, serial) {
	var message = new dh.chat.Message(userId, version, name, text, timestamp, serial)
	this._messageList.addMessage(message)
}

// Clear all messages and users (called on reconnect)
dh.framer.onReconnect = function() {
    this._personDivs = {}

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

dh.framer._addUser = function(user, before) {
    var userList = document.getElementById("dhChatUserList")
    
    user.span = document.createElement("span")
    user.span.appendChild(document.createTextNode(user.name))
    
	userList.insertBefore(user.span, before)
	if (user.span.nextSibling)
		userList.insertBefore(document.createTextNode(", "), user.span.nextSibling)
	else if (user.span.previousSibling)
		userList.insertBefore(document.createTextNode(", "), user.span)
}

dh.framer._removeUser = function(user) {
    var userList = document.getElementById("dhChatUserList")
    if (user.span.nextSibling)
	    userList.removeChild(user.span.nextSibling)
    userList.removeChild(user.span)
}

dh.framer.setSelfId = function(id) {
	this._selfId = id
}

dh.framer.init = function() {
}
