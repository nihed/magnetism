dojo.provide("dh.chatwindow")

dojo.require("dh.control")
dojo.require("dh.chat")
dojo.require("dh.dom")
dojo.require("dh.event")
dojo.require("dh.chatinput")
dojo.require("dh.util")
dojo.require("dojo.event")

dh.chatwindow.PHOTO_SIZE = 30
dh.chatwindow.MAX_HISTORY_COUNT = 110

// Set from the .jsp page
dh.chatwindow.chatId = null
dh.chatwindow.selfId = null

// True if we're currently maintaining the scrollbar at the bottom
// of the window; we update this only when the user explicitly scrolls
dh.chatwindow._atBottom = true;

dh.chatwindow._createHeadShot = function(photoUrl) {
    return dh.util.createPngElement(photoUrl, dh.chatwindow.PHOTO_SIZE, dh.chatwindow.PHOTO_SIZE);
}

// Check if we are scrolled to the bottom (with 8 pixels of fuzz)
dh.chatwindow._isAtBottom = function(element) {
	return element.scrollTop >= element.scrollHeight - element.clientHeight - 8;
}

// Scroll to the bottom
dh.chatwindow._scrollToBottom = function(element) {
	element.scrollTop = element.scrollHeight - element.clientHeight;
}

dh.chatwindow._messageElementId = function(message) {
	return "dhChatMessage-" + message.getSerial();
}

dh.chatwindow._messageElement = function(message) {
	return document.getElementById(this._messageElementId(message));
}

dh.chatwindow._messageTimeElementId = function(message) {
	return "dhChatMessageTime-" + message.getSerial();
}

dh.chatwindow._messageTimeElement = function(message) {
	return document.getElementById(this._messageTimeElementId(message));
}

dh.chatwindow._addMessage = function(message, before) {
    var userUrl = "/person?who=" + message.getEntity().getId()
    
	var messageElement = document.createElement("div")
	messageElement.className = message.userFirst ? "dh-chat-message dh-chat-message-userfirst" : "dh-chat-message";
	messageElement.id = this._messageElementId(message)
    
    if (message.userFirst) {
		var photoUrl = dh.util.sizePhoto(message.getEntity().getPhotoUrl(), dh.chatwindow.PHOTO_SIZE)
	    var image = this._createHeadShot(photoUrl);
	    image.title = message.getEntity().getName();
	    var linkElement = dh.util.createLinkElementWithChild(userUrl, image)       
	    linkElement.className = "dh-chat-message-photo";
	    messageElement.appendChild(linkElement)
    } else {
    	var div = document.createElement("div");
    	div.className = "dh-chat-message-photo";
	    messageElement.appendChild(div);
    }
    
    var rightDiv = document.createElement("div");
	rightDiv.className = "dh-chat-message-right";
	messageElement.appendChild(rightDiv);

	var sentimentDiv = document.createElement("div");
	sentimentDiv.className = "dh-chat-message-sentiment";
	rightDiv.appendChild(sentimentDiv);

	if (message.sentimentFirst) {
	
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
	}

	var contentsDiv = document.createElement("div");
	contentsDiv.className = "dh-chat-message-contents";
	rightDiv.appendChild(contentsDiv);
	
    var textSpan = document.createElement("span");
    textSpan.className = "dh-chat-message-text";
    dh.util.insertTextWithLinks(textSpan, message.getMessage());
	contentsDiv.appendChild(textSpan);

	contentsDiv.appendChild(document.createTextNode(" - "));
	
    var whoElement = dh.util.createLinkElement(userUrl, message.getEntity().getName());
	whoElement.className = "dh-chat-message-who";
	contentsDiv.appendChild(whoElement);
	
	contentsDiv.appendChild(document.createTextNode(" "));

	var timeSpan = document.createElement("span");
	timeSpan.className = "dh-chat-message-time";
	timeSpan.id = this._messageTimeElementId(message);
	timeSpan.appendChild(document.createTextNode(message.timeString()));
	contentsDiv.appendChild(timeSpan);

	var wasAtBottom = this._atBottom;

    var messagesDiv = document.getElementById("dhChatMessages");
    messagesDiv.insertBefore(messageElement, before ? this._messageElement(before) : null);
		
    if (wasAtBottom) {
		this._scrollToBottom(messagesDiv);
		this._atBottom = true;
	}
				
	try {
		window.external.application.DemandAttention()
	} catch (e) {
	}
}

dh.chatwindow._removeMessage = function(message) {
    var messagesDiv = document.getElementById("dhChatMessages")
    var messageElement = this._messageElement(message)
	messagesDiv.removeChild(messageElement)
}

dh.chatwindow._userElementId = function(user) {
	return "dhChatUser-" + user.getId()
}

dh.chatwindow._userElement = function(user) {
	return document.getElementById(this._userElementId(user))
}

dh.chatwindow._addUser = function(user, before, participant) {
	var userList = document.getElementById("dhChatUserList")
    
    var userUrl = "/person?who=" + user.getId()
    var userElement = dh.util.createLinkElement(userUrl, user.getName());
    userElement.id = this._userElementId(user)

	userList.insertBefore(userElement, before ? this._userElement(before) : null)
	if (userElement.nextSibling)
		userList.insertBefore(document.createTextNode(", "), userElement.nextSibling);
	else if (userElement.previousSibling)
		userList.insertBefore(document.createTextNode(", "), userElement);
}

dh.chatwindow._removeUser = function(user) {
	var userList = document.getElementById("dhChatUserList")
	var userElement = this._userElement(user)
    
    if (userElement.nextSibling)
	    userList.removeChild(userElement.nextSibling)
    else if (userElement.previousSibling)
	    userList.removeChild(userElement.previousSibling)
    userList.removeChild(userElement)
}

dh.chatwindow._updateUser = function(user) {
	var userElement = this._userElement(user)
	userElement.replaceChild(document.createTextNode(user.getName()), userElement.firstChild)
}

dh.chatwindow.sendClicked = function() {
    var text = dh.chatinput.getText();
    if (text == "") {
        alert("Please enter a non-empty message");
        return;
    }

	dh.control.control.sendChatMessage(this.chatId, text, dh.chatinput.getSentiment());
	dh.chatinput.setText("");
    
    // Scroll to the bottom so that the user can see their own new message
    var messagesDiv = document.getElementById("dhChatMessages");
	this._scrollToBottom(messagesDiv);
    this._atBottom = true;
}

// Note that this handler is used directly and not invoked
// as a method with 'this'
dh.chatwindow.onBodyKeyPress = function (e) {
	var keycode = dh.event.getKeyCode(e);
	if (keycode == 27) {
    	dh.event.cancel(e);
		window.close();
    	return false;
	}
	
	return true;
}

// Called as an event handler, and not as a method with 'this'
dh.chatwindow._onMessagesScroll = function (e) {
    var messagesDiv = document.getElementById("dhChatMessages");
	dh.chatwindow._atBottom = dh.chatwindow._isAtBottom(messagesDiv);
	
	return true;
}

dh.chatwindow._createLists = function() {
	this._messageList = new dh.chat.MessageList(
		this._chatRoom,
		function(message, before) { dh.chatwindow._addMessage(message, before); },
		function(message) { dh.chatwindow._removeMessage(message); },
		dh.chatwindow.MAX_HISTORY_COUNT);
		
	this._participantList = new dh.chat.UserList(
		this._chatRoom,
		function(user, before, participant) { dh.chatwindow._addUser(user, before, true) },
		function(user) { dh.chatwindow._removeUser(user); },
		function(user) { dh.chatwindow._updateUser(user); },
		function(user, participant) { return true; });
}

dh.chatwindow.updateTimes = function() {
	dh.chatwindow._messageList.foreachMessage(function(message) {
		var span = dh.chatwindow._messageTimeElement(message);
		var currentText = dh.dom.textContent(span);
		var newText = message.timeString();
		if (newText != currentText); // Avoid spurious redraws
			dh.dom.textContent(span, newText);
	});
	
	// We avoid setInterval because of a bug on firefox-x86_64, already
	// fixed as of Firefox-2.0
	setTimeout(dh.chatwindow.updateTimes, 60 * 1000);
}

dh.chatwindow._init = function() {
	dh.control.createControl();
	
    dh.event.addEventListener(document.body, "keypress",
							  dh.chatwindow.onBodyKeyPress);
							  
	if (!dh.control.control.versionAtLeast("1.1.0")) {
		document.getElementById("dhChatTooOld").style.display = "block";
		return;
	}

    var messagesDiv = document.getElementById("dhChatMessages");
	dh.event.addEventListener(messagesDiv, "scroll",
							  dh.chatwindow._onMessagesScroll);

	this._chatRoom = dh.control.control.getOrCreateChatRoom(this.chatId)
	this._createLists()

	this._chatRoom.join(true)
	dh.chatinput.init();
	
	dh.chatinput.onCancel = function() {
    	dh.event.cancel(e);
    	window.close();
    }
    
    dh.chatinput.focus();

	setTimeout(dh.chatwindow.updateTimes, 60 * 1000);
}

dhChatwindowInit = function() {
	dh.chatwindow._init();
}
