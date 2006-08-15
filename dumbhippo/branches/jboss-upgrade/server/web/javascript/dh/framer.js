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
dh.framer.onUserJoin = function(userId, photoUrl, name, participant) {
	var user = new dh.chat.User(userId, photoUrl, name)
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
dh.framer.onMessage = function(userId, photoUrl, name, text, timestamp, serial) {
	var message = new dh.chat.Message(userId, photoUrl, name, text, timestamp, serial)
	this._messageList.addMessage(message)
}

// Clear all messages and users (called on reconnect)
dh.framer.onReconnect = function() {
	this._participantList.clear()
	this._visitorList.clear()
	this._messageList.clear()
	this._updateChatCount()
}

// Go back to the user's home page, possibly closing the browser bar
dh.framer.goHome = function() {
	var embed = document.getElementById("dhEmbedObject")
    if (embed && embed.readyState && embed.readyState >= 3)
    	embed.CloseBrowserBar()
	window.open("/", "_top")
}

dh.framer._addMessage = function(message, before) {
	message.nameDiv = document.createElement("div")
    message.nameDiv.className = "dh-chat-name"
  	message.nameDiv.appendChild(document.createTextNode(message.name))
  	 
    var messageFontStyle = dh.chat.getMessageFontStyle(message)
    
    message.div = document.createElement("div")
    message.div.className = "dh-chat-message"	
    dh.util.insertTextWithLinks(message.div, message.text) 
 	message.div.style.fontStyle = messageFontStyle
	
	var namesArea = document.getElementById('dhPostChatNames')
	namesArea.insertBefore(message.nameDiv, before ? before.nameDiv : null)
	var messageArea = document.getElementById('dhPostChatMessages')
	messageArea.insertBefore(message.div, before ? before.div : null)
}

dh.framer._removeMessage = function(message) {
	if (message.div && message.div.parentNode) {
	    message.div.parentNode.removeChild(message.div)
	    message.div = null
	}
	if (message.nameDiv && message.nameDiv.parentNode) {
	    message.nameDiv.parentNode.removeChild(message.nameDiv)
	    message.nameDiv = null;
	}
}

dh.framer._updateChatCount = function() {
	var chatCountNode = document.getElementById('dhPostChatCount')
	var count = dh.framer._participantList.numUsers() // only chatters, not viewers
	var countText;
	
	if (count == 0)
		countText = "(empty)"
	else if (count == 1)
		countText = "(1 person)"
	else
		countText = "(" + count + " people)"

	dojo.dom.textContent(chatCountNode, countText)
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

	dh.framer._updateChatCount()
}

dh.framer._removeUser = function(user, participant) {
	var userList = document.getElementById("dhPostViewingListPeople")
    
    if (user.span.nextSibling)
	    userList.removeChild(user.span.nextSibling)
    else if (user.span.previousSibling)
	    userList.removeChild(user.span.previousSibling)
    userList.removeChild(user.span)
    
    dh.framer._updateChatCount()
}

dh.framer.setSelfId = function(id) {
	this._selfId = id
}

dh.framer.init = function() {
	var chatControl = document.getElementById("dhChatControl")
    if (chatControl && chatControl.readyState && chatControl.readyState == 4) {
		chatControl.Rescan()
	} else {
		// NOTE this is currently partially handled in the jsp, where 
		// we have non-activex fallbacks sometimes. So be careful.
		
		// If we don't have the ActiveX controls available to chat, hide them
    	document.getElementById("dhPostSwarmInfo").style.visibility = "hidden"
    	document.getElementById("dhPostChatLog").style.visibility = "hidden"
    	// there's a fallback for this at least part of the time
    	// var joinChat = document.getElementById("dhPostJoinChat")
    	// if (joinChat)
	    //	joinChat.style.display = "none"
    }
}
