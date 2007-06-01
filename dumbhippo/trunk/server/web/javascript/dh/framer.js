dojo.provide("dh.framer");

dojo.require("dh.chat");
dojo.require("dh.control");
dojo.require("dh.util");
dojo.require("dojo.event");
dojo.require("dh.dom");

dh.framer._selfId = null

// Go back to the user's home page, possibly closing the browser bar
dh.framer.goHome = function() {
	if (dh.control.control.haveBrowserBar()) {
		dh.control.control.closeBrowserBar(dhBaseUrl);
		return;
	}

	var embed = document.getElementById("dhEmbedObject")
    if (embed && embed.readyState && embed.readyState >= 3)
    	embed.CloseBrowserBar()
	window.open(dhBaseUrl, "_top")
}

// Remove the frame, staying on the current page if possible
dh.framer.removeFrame = function() {
	if (dh.control.control.haveBrowserBar()) {
		dh.control.control.closeBrowserBar(null);
		return;
	}

	var embed = document.getElementById("dhEmbedObject")
    if (embed && embed.readyState && embed.readyState >= 3)
    	embed.CloseBrowserBar()
	window.open(this.forwardUrl, "_top")
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

dh.framer._messageTimeElementId = function(message) {
	return "dhChatMessageTime-" + message.getSerial();
}

dh.framer._messageTimeElement = function(message) {
	return document.getElementById(this._messageTimeElementId(message));
}

dh.framer._addMessage = function(message, before) {  	 
    var messageDiv = document.createElement("div")
	messageDiv.id = this._messageDivId(message)
    messageDiv.className = "dh-chat-message"	
    
    var sentimentDiv = document.createElement("div");
	sentimentDiv.className = "dh-chat-message-sentiment";
	messageDiv.appendChild(sentimentDiv);

	var sentimentUrl;
	var sentimentWidth = 11;
	var sentimentHeight = 11;
		
	switch(message.getSentiment()) {
	case dh.control.SENTIMENT_LOVE:
		sentimentUrl = dhImageRoot3 + "quiplove_icon.png";
		sentimentWidth = 12;
		break;
	case dh.control.SENTIMENT_HATE:
		sentimentUrl = dhImageRoot3 + "quiphate_icon.png";
		break;
	default:
		sentimentUrl = dhImageRoot3 + "comment_iconchat_icon.png";
		break;
	}
		
	var image = dh.util.createPngElement(sentimentUrl, sentimentWidth, sentimentHeight);
	sentimentDiv.appendChild(image);
    
    dh.util.insertTextWithLinks(messageDiv, message.getMessage()) 
		
    messageDiv.appendChild(document.createTextNode(" - "));
    
    var userUrl = "/person?who=" + message.getEntity().getId()    
	var whoElement = dh.util.createLinkElement(userUrl, message.getEntity().getName());
	whoElement.className = "dh-chat-message-who";
	whoElement.id = this._messageNameDivId(message)
	messageDiv.appendChild(whoElement);
	
	var timeSpan = document.createElement("span");
	timeSpan.className = "dh-chat-message-time";
	timeSpan.id = this._messageTimeElementId(message);
	timeSpan.appendChild(document.createTextNode(" " + message.timeString()));
	messageDiv.appendChild(timeSpan);
	
	var beforeMessageDiv = before ? this._messageDiv(before) : null;
	
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

dh.framer._addUser = function(user, before, participant) {
	var userList = document.getElementById("dhPostViewingListPeople");
	
    var userUrl = "/person?who=" + user.getId();
    var userElement = dh.util.createLinkElement(userUrl, user.getName());
            
    var span = document.createElement("span");
    span.id = this._userSpanId(user);	
    span.appendChild(userElement);

	userList.insertBefore(span, before ? this._userSpan(before) : null);
	if (span.nextSibling)
		userList.insertBefore(document.createTextNode(", "), span.nextSibling);
	else if (span.previousSibling)
		userList.insertBefore(document.createTextNode(", "), span);
}

dh.framer._removeUser = function(user) {
	var userList = document.getElementById("dhPostViewingListPeople")
	var span = this._userSpan(user)
    
    if (span.nextSibling)
	    userList.removeChild(span.nextSibling)
    else if (span.previousSibling)
	    userList.removeChild(span.previousSibling)
    userList.removeChild(span)
}

dh.framer._updateUser = function(user) {
	var span = this._userSpan(user)
	span.replaceChild(document.createTextNode(user.getName()), span.firstChild)
}

dh.framer.updateTimes = function() {
	dh.framer._messageList.foreachMessage(function(message) {
		var span = dh.framer._messageTimeElement(message);
		var currentText = dh.dom.textContent(span);
		var newText = " " + message.timeString();
		if (newText != currentText); // Avoid spurious redraws
			dh.dom.textContent(span, newText);
	});
	
	// We avoid setInterval because of a bug on firefox-x86_64, already
	// fixed as of Firefox-2.0
	setTimeout(dh.framer.updateTimes, 60 * 1000);
}

dh.framer._onMessage = function(message) {
    if (message.getSerial() > dh.framer.initialLastMessageId) {
        dh.framer.currentMessageCount++;
        var countSpan = document.getElementById('dhQuipsCount');
        countSpan.replaceChild(document.createTextNode(dh.framer.currentMessageCount), countSpan.firstChild);    
    }
}

dh.framer._onReconnect = function() {
    dh.framer.currentMessageCount = dh.framer.initialMessageCount;
}
	
dh.framer.init = function() {
	dh.control.createControl();

	this._chatRoom = dh.control.control.getOrCreateChatRoom(this.chatId)

	dojo.event.connect(this._chatRoom, "onMessage", this, "_onMessage")
	dojo.event.connect(this._chatRoom, "onReconnect", this, "_onReconnect")
	
	this._messageList = new dh.chat.MessageList(
		this._chatRoom,
		function(message, before) { dh.framer._addMessage(message, before) },
		function(message) { dh.framer._removeMessage(message) },
		3,
		true);
		
	this._userList = new dh.chat.UserList(
		this._chatRoom,
		function(user, before, participant) { dh.framer._addUser(user, before, true) },
		function(user) { dh.framer._removeUser(user) },
		function(user) { dh.framer._updateUser(user) },
		function(user, participant) { return true });

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
    } else {
	   	setTimeout(dh.framer.updateTimes, 60 * 1000);
   	}
}
