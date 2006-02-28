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

// Go back to the user's home page, possibly closing the browser bar
dh.framer.goHome = function() {
	var embed = document.getElementById("dhEmbedObject")
    if (embed && embed.readyState && embed.readyState >= 3)
    	embed.CloseBrowserBar()
	window.open("/home", "_self")
}

dh.framer._addMessage = function(message, before) {
	message.nameDiv = document.createElement("div")
    message.nameDiv.className = "dh-chat-name"
  	message.nameDiv.appendChild(document.createTextNode(message.name))
  	
  	message.div = document.createElement("div")
  	message.div.className = "dh-chat-message"
  	message.div.appendChild(document.createTextNode(message.text))
	
	var namesArea = document.getElementById('dhPostChatNames')
	namesArea.insertBefore(message.nameDiv, before ? before.nameDiv : null)
	var messageArea = document.getElementById('dhPostChatMessages')
	messageArea.insertBefore(message.div, before ? before.div : null)
}

dh.framer._removeMessage = function(message) {
	if (message.div && message.div.parent) {
	    message.div.parent.removeChild(message.div)
	    message.div = null
	}
	if (message.nameDiv && message.nameDiv.parent) {
	    message.nameDiv.parent.removeChild(message.nameDiv)
	    message.nameDiv = null;
	}
}

dh.framer._addUser = function(user, before, participant) {
	var userList = document.getElementById("dhPostViewingListPeople")
    
    user.span = document.createElement("span")
    user.span.appendChild(document.createTextNode(user.name))

	userList.insertBefore(user.span, before ? before.span : null)
	if (user.span.nextSibling)
		userList.insertBefore(document.createTextNode(", "), user.span.nextSibling);
	else if (user.span.previousSibling)
		userList.insertBefore(document.createTextNode(", "), user.span);
		
	var chatCountNode = document.getElementById('dhPostChatCount')
	var count = dh.framer._participantList.numUsers() // only chatters, not viewers
	var countText;
	
	if (count == 0)
		countText = "(nobody chatting)"
	else if (count == 1)
		countText = "(1 person)"
	else
		countText = "(" + count + " people)"
	
	dojo.dom.textContent(chatCountNode, countText)
}

dh.framer._removeUser = function(user, participant) {
	var userList = document.getElementById("dhPostViewingListPeople")
    
    if (user.span.nextSibling)
	    userList.removeChild(user.span.nextSibling)
    else if (user.span.previousSibling)
	    userList.removeChild(user.span.previousSibling)
    userList.removeChild(user.span)
}

dh.framer.setSelfId = function(id) {
	this._selfId = id
}

dh.framer.init = function() {
	var chatControl = document.getElementById("dhChatControl")
    if (chatControl && chatControl.readyState && chatControl.readyState == 4) {
		chatControl.Rescan()
	} else {
		// If we don't have the ActiveX controls available to chat, hide them
    	document.getElementById("dhPostViewingList").style.visibility = "hidden"
    	document.getElementById("dhPostChatLog").style.visibility = "hidden"
    	document.getElementById("dhPostChatLabel").style.visibility = "hidden"
    }
}
