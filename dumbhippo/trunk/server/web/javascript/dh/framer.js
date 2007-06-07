dojo.provide("dh.framer");

dojo.require("dh.chat");
dojo.require("dh.chatinput")
dojo.require("dh.control");
dojo.require("dh.util");
dojo.require("dojo.event");
dojo.require("dh.dom");

dh.framer._selfId = null

dh.framer._initialTitle = null
dh.framer._initialDescription = null
dh.framer._recipientsNode = null
dh.framer._whoIsAroundNode = null

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
    var i = 0;
    var userId = this._userSpanId(user);
    while (i < dh.framer._whoIsAroundNode.childNodes.length) {
        if (dh.framer._whoIsAroundNode.childNodes[i].id == userId)
            return dh.framer._whoIsAroundNode.childNodes[i];
        i++;
    }        
	return null;
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
    
    var sentimentSpan = document.createElement("span");
	sentimentSpan.className = "dh-chat-message-sentiment";
	messageDiv.appendChild(sentimentSpan);

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
	sentimentSpan.appendChild(image);
    
    var textSpan = document.createElement("span");
	textSpan.className = "dh-chat-message-text";
	messageDiv.appendChild(textSpan);
	    
    dh.util.insertTextWithLinks(textSpan, message.getMessage());
    // we use titles for both displaying the full text when the user rolls over
    // a message and for updating the visible text to a longer version
    // when the window is resized
    textSpan.title = message.getMessage();
		
    messageDiv.appendChild(document.createTextNode(" - "));
    
    var userUrl = "/person?who=" + message.getEntity().getId()    
	var whoElement = dh.util.createLinkElement(userUrl, message.getEntity().getName());
	whoElement.className = "dh-chat-message-who dh-entity-" + message.getEntity().getId();
	whoElement.id = this._messageNameDivId(message)
	messageDiv.appendChild(whoElement);
	
	var timeSpan = document.createElement("span");
	timeSpan.className = "dh-chat-message-time";
	timeSpan.id = this._messageTimeElementId(message);
	timeSpan.appendChild(document.createTextNode(" " + message.timeString()));
	messageDiv.appendChild(timeSpan);
	
	var beforeMessageDiv = before ? this._messageDiv(before) : null;
	
	var messageArea = document.getElementById('dhPostChatMessages');
	if (dh.framer.messagesInitialized)
	    messageDiv.style.visibility = "hidden";
	messageArea.insertBefore(messageDiv, beforeMessageDiv);

	var framerRight = document.getElementById('dhFramerRight');
	    
	if (dh.framer.messagesInitialized) {
	    // we reserve a 100 pixels for the sentiment, the '-', and the timestamp
	    // the timestamp can vary in length, but that whould not affect the length of the text
	    dh.util.ellipseText(textSpan, framerRight.offsetWidth - whoElement.offsetWidth - 100); 
	    messageDiv.style.visibility = "visible";	    
	} else if (message.getSerial() >= dh.framer.initialLastMessageId) {
	    // If we loaded in the initial last message, we are done initializing messages.
        // If we happened to get a 100 messages between the time we start loading
        // the framer page and we request the messages, we would not get the message
        // with the initial last message id, but will get a message with a greater 
        // message id. 
	    messageArea.style.display = "block";	
	    dh.framer.updateMessagesWidth();
        dh.framer.messagesInitialized = true;	   	
	}    
}

dh.framer.updateWidth = function() {
    dh.framer.updateBlockContentWidth();
    dh.framer.updateBlockDetails();
    dh.framer.updateWhoIsAround();
	dh.framer.updateMessagesWidth();			
}

dh.framer.updateBlockContentWidth = function() {
    var contentArea = document.getElementById('dhBlockContent');	
	var titleList = dh.html.getElementsByClass('dh-stacker-block-title', contentArea);
    if (titleList.length != 1)
	    throw "framer should contain a single title div";
    var titleLinkList = dh.html.getElementsByClass('dh-framer-title', titleList[0]);    	    
	var titleEllipsed = false;
	var titleLinkHeight = 0;
	if (titleLinkList.length > 0) {
	    // unless the block has no title, the title is the first link element in the contentArea
	    var titleLink = titleLinkList[0];
	    if (dh.framer._initialTitle == null) {
	        dh.framer._initialTitle = dh.util.getTextFromHtmlNode(titleLink);
	        titleEllipsed = dh.util.ellipseWrappingText(titleLink, 60, null, titleList[0]); 
	    } else {
	        titleEllipsed = dh.util.ellipseWrappingText(titleLink, 60, dh.framer._initialTitle, titleList[0]);
	    } 
	    titleLinkHeight = titleLink.offsetHeight;
	}    
	   
	var descriptionList = dh.html.getElementsByClass('dh-stacker-block-header-description', contentArea);
    if (descriptionList.length != 1)
	    throw "framer should contain a single block description";
	var description = descriptionList[0];    	
	if (dh.framer._initialDescription == null) { 
	    dh.framer._initialDescription = dh.util.getTextFromHtmlNode(description);
	    if (titleEllipsed) {
	        dh.dom.removeChildren(description);
	    } else {
	        dh.util.ellipseWrappingText(description, 60 - titleList[0].offsetHeight); 
	    } 
	} else {   	    
	    if (titleEllipsed) {
	        dh.dom.removeChildren(description);
	    } else {
	        dh.util.ellipseWrappingText(description, 60 - titleList[0].offsetHeight, dh.framer._initialDescription); 
	    } 
	}    	    
}

dh.framer.updateBlockDetails = function() {
	var framerLeft = document.getElementById('dhFramerLeft');		
    var blockInfo = document.getElementById('dhBlockInfoTable');	
	var entityListList = dh.html.getElementsByClass('dh-entity-list', blockInfo);
    if (entityListList.length != 2)
	    throw "block info should contain two entity lists: one for the sender and one for recipients";
	var senderSpan = entityListList[0];
	if (dh.framer._recipientsNode == null) {
	    dh.framer._recipientsNode = entityListList[1].cloneNode(true);
	    // 135 is for 'From', 'Sent to', and the timestamp
	    dh.util.ellipseNodeWithChildren(entityListList[1], framerLeft.offsetWidth - senderSpan.offsetWidth - 135, null, 2);
	} else {
	    dh.util.ellipseNodeWithChildren(entityListList[1], framerLeft.offsetWidth - senderSpan.offsetWidth - 135, dh.framer._recipientsNode, 2);
	}    	    	
}

dh.framer.updateWhoIsAround = function() {
	var framerLeft = document.getElementById('dhFramerLeft');		
	var whoIsAround = document.getElementById('dhPostViewingListPeople');
    if (dh.framer._whoIsAroundNode == null) {
        dh.framer._whoIsAroundNode = whoIsAround.cloneNode(true);
        // we normally don't expect anyone to be around when we are initializing this, but just in case
        // 80 is for 'Who's around:'
	    dh.util.ellipseNodeWithChildren(whoIsAround, framerLeft.offsetWidth - 80, null, 2);   
    } else {
	    dh.util.ellipseNodeWithChildren(whoIsAround, framerLeft.offsetWidth - 80, dh.framer._whoIsAroundNode, 2);   
	}     
}

dh.framer.updateMessagesWidth = function() {   		
	var framerRight = document.getElementById('dhFramerRight');		
    var messageArea = document.getElementById('dhPostChatMessages')   
	var chatMessages = dh.html.getElementsByClass('dh-chat-message', messageArea);
	var i = 0;
	while (i < chatMessages.length) {
	    var chatMessageText = dh.html.getElementsByClass('dh-chat-message-text', chatMessages[i]);	
        if (chatMessageText.length != 1)
	        throw "chat message div should contain a single chat message text element";
	        
	    var chatMessageWho = dh.html.getElementsByClass('dh-chat-message-who', chatMessages[i]);	
        if (chatMessageWho.length != 1)
	        throw "chat message div should contain a single chat message who element";
 	        
        dh.util.ellipseText(chatMessageText[0], framerRight.offsetWidth - chatMessageWho[0].offsetWidth - 100, chatMessageText[0].title); 
        i++;
    }
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
    var userUrl = "/person?who=" + user.getId();
    var userElement = dh.util.createLinkElement(userUrl, user.getName());
            
    var span = document.createElement("span");
    span.id = this._userSpanId(user);	
    span.appendChild(userElement);

	dh.framer._whoIsAroundNode.insertBefore(span, before ? this._userSpan(before) : null);
	if (span.nextSibling)
		dh.framer._whoIsAroundNode.insertBefore(document.createTextNode(", "), span.nextSibling);
	else if (span.previousSibling)
		dh.framer._whoIsAroundNode.insertBefore(document.createTextNode(", "), span);
	dh.framer.updateWhoIsAround();		
}

dh.framer._removeUser = function(user) {
	var span = this._userSpan(user)
    
    if (span.nextSibling)
	    dh.framer._whoIsAroundNode.removeChild(span.nextSibling)
    else if (span.previousSibling)
	    dh.framer._whoIsAroundNode.removeChild(span.previousSibling)
    dh.framer._whoIsAroundNode.removeChild(span)

	dh.framer.updateWhoIsAround();
}

dh.framer._updateUser = function(user) {
	var span = this._userSpan(user)
    var link = span.firstChild
	link.replaceChild(document.createTextNode(user.getName()), link.firstChild)

	dh.framer.updateWhoIsAround();
	
	var entityClassName = "dh-entity-" + user.getId();
	var blockSender = document.getElementById("dhBlockSender");
	var updateBlockDetails = false;
	if (blockSender.className == entityClassName) {
	    blockSender.replaceChild(document.createTextNode(user.getName()), blockSender.firstChild);
	    updateBlockDetails = true;
	}
	
	var recipientList = dh.html.getElementsByClass(entityClassName, dh.framer._recipientsNode);
	// we expect 0 or 1 occurrences, if the person received a block because they were in some group
	// it was sent to, the person's name would not be on the list of recipients 
	if (recipientList.length == 1) {
	    recipientList[0].replaceChild(document.createTextNode(user.getName()), recipientList[0].firstChild);
        updateBlockDetails = true;
    }	    
    
    if (updateBlockDetails) 
        dh.framer.updateBlockDetails();
        
    var messageArea = document.getElementById('dhPostChatMessages');
    messageSenders = dh.html.getElementsByClass(entityClassName, messageArea);
	var i = 0;
	while (i < messageSenders.length) {
	    messageSenders[i].replaceChild(document.createTextNode(user.getName()), messageSenders[i].firstChild);
	    i++;	
	}    
	if (i > 0)
	    dh.framer.updateMessagesWidth();
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
    dh.framer.messagesInitialized = false;
    if (dh.framer.initialMessageCount <= 0) {
	    var messageArea = document.getElementById('dhPostChatMessages');     
	    messageArea.style.display = "block";
        dh.framer.messagesInitialized = true;	       
    }

    dh.framer.updateBlockContentWidth();
    dh.framer.updateBlockDetails();
    dh.framer.updateWhoIsAround();
            
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
	
	dh.chatinput.init();
	
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
   	
   	// we decided not to include the title in the framer quip popup
   	// var quipper = document.getElementById("dhQuipper");
	// quipper.dhTitle = dh.framer.title;
}
