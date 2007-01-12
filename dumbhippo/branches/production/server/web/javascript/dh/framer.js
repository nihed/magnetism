dojo.provide("dh.framer");

dojo.require("dh.chat");
dojo.require("dh.control");
dojo.require("dh.util");
dojo.require("dojo.event");
dojo.require("dh.dom");

dh.framer._selfId = null

// Go back to the user's home page, possibly closing the browser bar
dh.framer.goHome = function() {
	var embed = document.getElementById("dhEmbedObject")
    if (embed && embed.readyState && embed.readyState >= 3)
    	embed.CloseBrowserBar()
	window.open("/", "_top")
}

dh.framer.setSelfId = function(id) {
	this._selfId = id
}

dh.framer.openForwardWindow = function() {
	dh.util.openShareLinkWindow(this.forwardUrl, this.forwardTitle);
}

dh.framer._messageDivId = function(message) {
	return "dhPostChatDiv-" + message.getSerial()
}

dh.framer._messageDiv = function(message) {
	return document.getElementById(this._messageDivId(message))
}

dh.framer._messageNameDivId = function(message) {
	return "dhPostChatNameDiv-" + message.getSerial()
}

dh.framer._messageNameDiv = function(message) {
	return document.getElementById(this._messageNameDivId(message))
}

dh.framer._userSpanId = function(user) {
	return "dhPostUserSpan-" + user.getId()
}

dh.framer._userSpan = function(user) {
	return document.getElementById(this._userSpanId(user))
}

dh.framer._addMessage = function(message, before) {
	var nameDiv = document.createElement("div")
    nameDiv.id = this._messageNameDivId(message)
    nameDiv.className = "dh-chat-name"
  	nameDiv.appendChild(document.createTextNode(message.getEntity().getName()))
  	 
    var messageFontStyle = dh.chat.getMessageFontStyle(message)
    
    var messageDiv = document.createElement("div")
	messageDiv.id = this._messageDivId(message)
    messageDiv.className = "dh-chat-message"	
    dh.util.insertTextWithLinks(messageDiv, message.getMessage()) 
 	messageDiv.style.fontStyle = messageFontStyle
	
	var beforeMessageDiv = before ? this._messageDiv(before) : null;
	var beforeNameDiv = before ? this._messageNameDiv(before) : null;
	
	var namesArea = document.getElementById('dhPostChatNames')
	namesArea.insertBefore(nameDiv, beforeNameDiv)
	var messageArea = document.getElementById('dhPostChatMessages')
	messageArea.insertBefore(messageDiv, beforeMessageDiv)
}

dh.framer._removeMessage = function(message) {
	var messageDiv = this._messageDiv(message)
	var messageNameDiv = this._messageNameDiv(message)

	if (messageDiv && messageDiv.parentNode) {
	    messageDiv.parentNode.removeChild(messageDiv)
	}
	if (messageNameDiv && messageNameDiv.parentNode) {
	    messageNameDiv.parentNode.removeChild(messageNameDiv)
	    messageNameDiv = null;
	}
}

dh.framer._updateChatCount = function() {
	var chatCountNode = document.getElementById('dhPostChatCount')
	if (!chatCountNode)
		return;
	
	var count = this._participantList.numUsers() // only chatters, not viewers
	var countText;
	
	if (count == 0)
		countText = "(empty)"
	else if (count == 1)
		countText = "(1 person)"
	else
		countText = "(" + count + " people)"

	dh.dom.textContent(chatCountNode, countText)
}

dh.framer._addUser = function(user, before, participant) {
	var userList = document.getElementById("dhPostViewingListPeople")
    
    var span = document.createElement("span")
    span.id = this._userSpanId(user)
    span.appendChild(document.createTextNode(user.getName()))

	userList.insertBefore(span, before ? this._userSpan(before) : null)
	if (span.nextSibling)
		userList.insertBefore(document.createTextNode(", "), span.nextSibling);
	else if (span.previousSibling)
		userList.insertBefore(document.createTextNode(", "), span);

	this._updateChatCount()
}

dh.framer._removeUser = function(user) {
	var userList = document.getElementById("dhPostViewingListPeople")
	var span = this._userSpan(user)
    
    if (span.nextSibling)
	    userList.removeChild(span.nextSibling)
    else if (span.previousSibling)
	    userList.removeChild(span.previousSibling)
    userList.removeChild(span)
    
    this._updateChatCount()
}

dh.framer._updateUser = function(user) {
	var span = this._userSpan(user)
	span.replaceChild(document.createTextNode(user.getName()), span.firstChild)
}

dh.framer.init = function() {
	dh.control.createControl();

	this._chatRoom = dh.control.control.getOrCreateChatRoom(this.chatId)

	this._messageList = new dh.chat.MessageList(
		this._chatRoom,
		function(message, before) { dh.framer._addMessage(message, before) },
		function(message) { dh.framer._removeMessage(message) },
		3);
		
	this._participantList = new dh.chat.UserList(
		this._chatRoom,
		function(user, before, participant) { dh.framer._addUser(user, before, true) },
		function(user) { dh.framer._removeUser(user) },
		function(user) { dh.framer._updateUser(user) },
		function(user, participant) { return participant });

	dh.framer._visitorList = new dh.chat.UserList(
		this._chatRoom,
		function(user, before, participant) { dh.framer._addUser(user, before, false) },
		function(user) { dh.framer._removeUser(user) },
		function(user) { dh.framer._updateUser(user) },
		function(user, participant) { return !participant });
		
	this._chatRoom.join(false)

	if (!dh.control.control.haveLiveChat()) {
		// NOTE this is currently partially handled in the jsp, where 
		// we have non-activex fallbacks sometimes. So be careful.
		
    	document.getElementById("dhPostSwarmInfo").style.visibility = "hidden"
    	document.getElementById("dhPostChatLog").style.visibility = "hidden"
    	// there's a fallback for this at least part of the time
    	// var joinChat = document.getElementById("dhPostJoinChat")
    	// if (joinChat)
	    //	joinChat.style.display = "none"
    }
}
