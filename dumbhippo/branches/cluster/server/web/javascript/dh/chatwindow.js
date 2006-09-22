dojo.provide("dh.chatwindow")

dojo.require("dh.chat")
dojo.require("dh.util")
dojo.require("dojo.event")

dh.chatwindow._selfId = null
// _textAreaWidth contains the width of the area available for chat messages,
// it is current between resizings, but should be updated only after resizing
// of text messages is complete, because during the process we need to know
// the previous text area width
dh.chatwindow._textAreaWidth = null
dh.chatwindow.PHOTO_WIDTH = 60
dh.chatwindow.FLOAT_ADJUSTMENT = 3
dh.chatwindow.MESSAGES_DIV_SIDE_PADDING = 2
dh.chatwindow.MESSAGES_FONT_FAMILY = "Verdana, sans"  
dh.chatwindow.MAX_HISTORY_COUNT = 110

dh.chatwindow._createHeadShot = function(photoUrl) {
    return dh.util.createPngElement(photoUrl, dh.chatwindow.PHOTO_WIDTH, dh.chatwindow.PHOTO_WIDTH)
}

// Check if we are scrolled to the bottom (with 8 pixels of fuzz)
dh.chatwindow._isAtBottom = function(element) {
	return element.scrollTop >= element.scrollHeight - element.clientHeight - 8
}

// Scroll to the bottom
dh.chatwindow._scrollToBottom = function(element) {
	element.scrollTop = element.scrollHeight - element.clientHeight
}

dh.chatwindow._messageDivId = function(message) {
	return "dhPostChatDiv-" + message.getSerial()
}

dh.chatwindow._messageDiv = function(message) {
	return document.getElementById(this._messageDivId(message))
}

dh.chatwindow._addMessage = function(message, before, resizingFlag) {
	dh.util.clientDebug("adding chat message: " + message + " resizing: " + resizingFlag)
	var messageDiv = document.createElement("div")
	messageDiv.className = "dh-chat-message"
	messageDiv.id = this._messageDivId(message)
	var isOwnMessage = message.getEntity().getId() == this._selfId
	
    if (isOwnMessage)
        messageDiv.className += " dh-chat-message-my"
    else
        messageDiv.className += " dh-chat-message-other"
    if (message.userFirst)
        messageDiv.className += " dh-chat-message-user-first"
	else
        messageDiv.className += " dh-chat-message-user-repeat"
    
    var image = this._createHeadShot(message.getEntity().getPhotoUrl())
    image.className = "dh-chat-message-image"
    image.title = message.getEntity().getName()
    var userUrl = "/person?who=" + message.getEntity().getId()
    var linkElement = dh.util.createLinkElementWithChild(userUrl, image)       
    messageDiv.appendChild(linkElement)

    var messagesDiv = document.getElementById("dhChatMessagesDiv")      
	var wasAtBottom = this._isAtBottom(messagesDiv)
    messagesDiv.insertBefore(messageDiv, before ? this._messageDiv(before) : null)
		
    var textDiv = document.createElement("div")
    textDiv.className = "dh-chat-message-text"

    var messageFontStyle = dh.chat.getMessageFontStyle(message)
    var textSidePadding = 8
    var textPadding = 2
    
    var textWidth = dh.util.getTextWidth(message.getMessage(), this.MESSAGES_FONT_FAMILY, null, messageFontStyle) + textSidePadding*2
 
    // because this function can be called as part of the resizing process, we do
    // not use the _textAreaWidth, but rather we use the textAreaWidth calculated
    // based on the space currently available to the messagesDiv
    var textAreaWidth = this._calculateTextAreaWidth(messagesDiv.offsetWidth)
    if (textWidth > textAreaWidth) {
       textWidth = textAreaWidth
	}	
	
    textDiv.style.width = textWidth + "px"    
    // these could have been placed in chatwindow.css, but it's better to have
    // them here because textWidth depends on the value of textSidePadding
    textDiv.style.paddingRight = textSidePadding + "px"
    textDiv.style.paddingLeft = textSidePadding + "px"    
    textDiv.style.paddingTop = textPadding + "px"
    textDiv.style.paddingBottom = textPadding + "px"   
         
    messageDiv.appendChild(textDiv)         
    var textSpan = document.createElement("span")		
    dh.util.insertTextWithLinks(textSpan, message.getMessage())
    textSpan.className = "dh-chat-message-text-inner"
    textSpan.style.fontFamily = this.MESSAGES_FONT_FAMILY  
    textSpan.style.fontStyle = messageFontStyle
    textDiv.appendChild(textSpan)
    
    // readjust the right padding, if necessary
    // for some reason, no matter how long the message that is displayed in italics is,
    // if it takes up less than one line, there are 4 pixels added on to it on the right
    // we subtract these extra pixels (texDiv.offsetWidth - textWidth) from the padding
    // on the right to get the message nicely centered
    // just to be extra cautious, we do not do this if textDiv.offsetWidth - textWidth
    // is greater than textSidePadding
    if ((messageFontStyle == dh.chat.DESCRIPTION_MESSAGE_FONT_STYLE) && (textDiv.offsetWidth - textWidth <= textSidePadding))
        textDiv.style.paddingRight = (textSidePadding - (textDiv.offsetWidth - textWidth)) + "px"       
		
    if (isOwnMessage) {   
        if (!message.userFirst) {
            // inline content that is next to the photo is offset to the right
            // by 3 pixels in some mysterious way. (This offset occurs even if
            // the margin is much bigger than the width of the photo!) To make
            // everything line up, we first set the margin to an initial value
            // that doesn't account for this 3 pixel offset. If we actually
            // end up with the inline content positioned where we asked for,
            // than we must be past the end of the photo, so we set the margin
            // again to a value that explicitly includes the 3 pixel offset.
            // We use this.FLOAT_ADJUSTMENT to store the value of this 3 pixel
            // adjustment we need to make. 
            // TODO: for some reason this trick has stopped working when messages
            // are initially loaded, it still works when they are being typed in.
            // This was not affected by the introduced side padding, as it is being
            // taken into account.
            messageDiv.style.marginLeft = this.PHOTO_WIDTH + "px"         
            
            if (textDiv.offsetLeft == this.PHOTO_WIDTH + this.MESSAGES_DIV_SIDE_PADDING) {
               messageDiv.style.marginLeft = this.PHOTO_WIDTH + this.FLOAT_ADJUSTMENT  + "px"               
            }
        }
    }
    else {
        messageDiv.style.marginLeft = (textAreaWidth - textWidth) + this.PHOTO_WIDTH + this.FLOAT_ADJUSTMENT + "px" 
    }
	
	// if this is not a first message in a given batch of messages, make sure that they all are
	// the same width, which should be the width of the longest message in this batch
	if (!message.userFirst) {
	    var doneWithPrevSiblings = false
	    var prevSibling = messageDiv.previousSibling
	    // compare sizes with previus siblings 
	    while (!doneWithPrevSiblings && prevSibling) {	     	    
            if (prevSibling.lastChild.offsetWidth < textWidth) {  
			    // the width of a new message is going to be longer than the width
                // of previous messages in this batch   
                // in this case we need to go through all the previous siblings 
                // and update them, which is why we are not going to set 
                // doneWithPrevSiblings to true here	            
                prevSibling.lastChild.style.width = textDiv.offsetWidth
                if (!isOwnMessage) {
                    prevSibling.style.marginLeft = messageDiv.style.marginLeft
                }	
            } else {
                // the width of a new message is shorter or the same as the width
                // of previous messages, so we should make sure it is the same 
                textDiv.style.width = prevSibling.lastChild.offsetWidth		
                if (!isOwnMessage) {
                    messageDiv.style.marginLeft = prevSibling.style.marginLeft
                }
                doneWithPrevSiblings = true 
			}	    			                
			if (prevSibling.className.indexOf("dh-chat-message-user-first") >= 0) {
			    doneWithPrevSiblings = true    
			}
            prevSibling = prevSibling.previousSibling  
        }    
	}
		 
		
	// if before is specified and the messages are from the same user, we have subsequent 
	// siblings we need to take care of 
	//
	// we are going to ignore whether or not before is the first message from that particular 
	// user (has type "dh-chat-message-user-first"), because if it claims to be the first one, 
	// this is the next thing that gets changed in addMessage function for the MessageList in 
	// chat.js
	// on the other hand, if we encounter another subsequent message that has the type 
	// "dh-chat-message-user-first", that's when we know we need to stop readjusting sizes
	//
	// if we are resizing, we always do that from first message to last, so evaluating the
	// size of subsequent messages is not necessary. In particular, we do not want to make 
	// a message as long as its subsequent messages in cases when it actually needs to be 
	// shorter due to resizing 
	if (!resizingFlag && before && (before.getEntity() == message.getEntity())) {
		var doneWithNextSiblings = false
	    var nextSibling = messageDiv.nextSibling
	    // compare sizes with next siblings 
	    while (!doneWithNextSiblings && nextSibling) {	     	    
            if (nextSibling.lastChild.offsetWidth < textWidth) {  
			    // the width of a new message is going to be longer than the width
                // of next messages in this batch   
                // in this case we need to go through all the next siblings 
                // and update them, which is why we are not going to set 
                // doneWithNextSiblings to true here	            
                nextSibling.lastChild.style.width = textDiv.offsetWidth
                if (!isOwnMessage) {
                    nextSibling.style.marginLeft = messageDiv.style.marginLeft
                }	
            } else {
                // the width of a new message is shorter or the same as the width
                // of next messages, so we should make sure it is the same 
                textDiv.style.width = nextSibling.lastChild.offsetWidth		            			            
                if (!isOwnMessage) {
                    messageDiv.style.marginLeft = nextSibling.style.marginLeft
                }
                doneWithNextSiblings = true 
			}
				    
			// when we were evaluating previous siblings, we wanted to readjust the
			// size of the one that is of type "dh-chat-message-user-first", but
			// when we are evaluating subsequent siblings, we want to stop before
			// the next one of type "dh-chat-message-user-first"
								                
			nextSibling = nextSibling.nextSibling
						                
			if (nextSibling && nextSibling.className.indexOf("dh-chat-message-user-first") >= 0) {
			    doneWithNextSiblings = true    
			}
        }  
	} 
    
    if (!before && wasAtBottom)
		this._scrollToBottom(messagesDiv)
				
	try {
		window.external.application.DemandAttention()
	} catch (e) {
	}
}

dh.chatwindow._removeMessage = function(message) {
    var messagesDiv = document.getElementById("dhChatMessagesDiv")
    var messageDiv = this._messageDiv(message)
	messagesDiv.removeChild(messageDiv)
}

dh.chatwindow._resizeMessage = function(message, before) {
    var messagesDiv = document.getElementById("dhChatMessagesDiv")   
    var messageDiv = this._messageDiv(message)
    var messageInput = document.getElementById("dhChatMessageInput")
    var oldTextAreaWidth = this._textAreaWidth
    var newTextAreaWidth = this._calculateTextAreaWidth(messagesDiv.offsetWidth)
    // last child of a message div is the textDiv
    var oldTextWidth = messageDiv.lastChild.offsetWidth
    // if the message was wrapped before (oldTextWidth is not less than oldTextAreaWidth) 
    // or it wasn't wrapped before but exceeds the new width (oldTextWidth is greater than 
    // newTextAreaWidth), then changing the width of the window should change the width of
    // the message, so we need to recalculate its size
    if ((oldTextWidth >= oldTextAreaWidth) || (oldTextWidth > newTextAreaWidth)) {
        dh.chatwindow._removeMessage(message)
        dh.chatwindow._addMessage(message, before, true)
    } else if (message.getEntity().getId() != this._selfId) {   
        // we are happy with the curent text width, but in the case of messages from other people 
        // that "float" right, we need to readjust the left margin based on the new textAreaWidth 
        messageDiv.style.marginLeft = (newTextAreaWidth - oldTextWidth) + this.PHOTO_WIDTH + this.FLOAT_ADJUSTMENT + "px"      	
    }   
}

dh.chatwindow._userDivId = function(user) {
	return "dhPostUserDiv-" + user.getId()
}

dh.chatwindow._userDiv = function(user) {
	return document.getElementById(this._userDivId(user))
}

dh.chatwindow._addUser = function(user, before) {
    var userDiv = document.createElement("div")
    userDiv.id = this._userDivId(user)

	var imageDiv = document.createElement("div")
    imageDiv.className = "dh-chat-person-image"
	userDiv.appendChild(imageDiv)

    var nameDiv = document.createElement("div")
    nameDiv.className = "dh-chat-person-name"
    userDiv.appendChild(nameDiv)

    var nameSpan = document.createElement("span")
    nameSpan.className = "dh-chat-person-name-inner"
    nameDiv.appendChild(nameSpan)

	var arrangementNameDiv = document.createElement("div")
    arrangementNameDiv.className = "dh-chat-person-arrangement-name"
    userDiv.appendChild(arrangementNameDiv)
    
    var arrangementNameSpan = document.createElement("span")
    arrangementNameSpan.className = "dh-chat-person-arrangement-name-inner"
    arrangementNameDiv.appendChild(arrangementNameSpan)

    var artistDiv = document.createElement("div")
    artistDiv.className = "dh-chat-person-artist"
    userDiv.appendChild(artistDiv)

    var artistSpan = document.createElement("span")
    artistSpan.className = "dh-chat-person-artist-inner"        
    artistDiv.appendChild(artistSpan)
    
    this._updateUserDiv(user, userDiv)

    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    peopleDiv.insertBefore(userDiv, before ? this._userDiv(before) : null) 
}

dh.chatwindow._removeUser = function(user) {
    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    var userDiv = this._userDiv(user)
    peopleDiv.removeChild(userDiv)
}

dh.chatwindow._updateUser = function(user) {
	var userDiv = this._userDiv(user);
	if (userDiv)
		this._updateUserDiv(user, userDiv);
}

dh.chatwindow._findChild = function(parentNode, className) {
    var children = parentNode.childNodes;
    for (var i = 0; i < children.length; i++) {
    	if (children[i].className == className)
    		return children[i];
    }
    return null;
}

dh.chatwindow._replaceFirst = function(parentNode, child) {
	if (parentNode.firstChild)
		parentNode.replaceChild(child, parentNode.firstChild);
	else
		parentNode.appendChild(child);
}

dh.chatwindow._updateUserDiv = function(user, userDiv) {
    var arrangementName = user.getCurrentSong()
    var artist = user.getCurrentArtist()
    var musicPlaying = user.getMusicPlaying()

    var useOldMusicInfo = ((arrangementName == "") && (artist == "") && !musicPlaying)
    if (musicPlaying) {
	    userDiv.className = "dh-chat-person dh-chat-person-music-playing"
    } else {
	    userDiv.className = "dh-chat-person dh-chat-person-music-stopped"
    }

    var userUrl = "/person?who=" + user.getId()

	var imageDiv = this._findChild(userDiv, "dh-chat-person-image")
    var image = this._createHeadShot(user.getPhotoUrl())        
    image.title = user.getName()
    var linkElement = dh.util.createLinkElementWithChild(userUrl, image)
    this._replaceFirst(imageDiv, linkElement)
    
	var nameDiv = this._findChild(userDiv, "dh-chat-person-name")
    nameDiv.title = user.getName()
    var nameLink = dh.util.createLinkElement(userUrl, user.getName())
    this._replaceFirst(nameDiv.firstChild, nameLink)
   
	var arrangementNameDiv = this._findChild(userDiv, "dh-chat-person-arrangement-name")
    arrangementNameDiv.title = arrangementName
    var arrangementLinkUrl = "/artist?track=" + arrangementName + "&artist=" + artist 
    var arrangementLink = dh.util.createLinkElement(arrangementLinkUrl, arrangementName)        
    this._replaceFirst(arrangementNameDiv.firstChild, arrangementLink)

	var artistDiv = this._findChild(userDiv, "dh-chat-person-artist")
    artistDiv.title = artist
    var artistLink = dh.util.createLinkElement(arrangementLinkUrl, artist)    
    this._replaceFirst(artistDiv.firstChild, artistLink)
}

// Adjust element sizes for the current window size; we need to do this
// manually since some things we want aren't possible with pure CSS,
// especially with the IE limitations
dh.chatwindow.resizeElements = function() {
	dh.util.clientDebug("resizing chat window elements");
    var width, height
    if (window.innerWidth) {
        width = window.innerWidth
        height = window.innerHeight
    } else {
        width = document.body.offsetWidth
        height = document.body.offsetHeight
    }
    
    var postInfoDiv = document.getElementById("dhChatPostInfoDiv")
    var peopleContainer = document.getElementById("dhChatPeopleContainer")
    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    var messagesContainer = document.getElementById("dhChatMessagesContainer")    
    var messagesDiv = document.getElementById("dhChatMessagesDiv")
    var messageInput = document.getElementById("dhChatMessageInput")
    var sendButtonDiv = document.getElementById("dhChatSendButtonArea")
    
    // to make things align, the offsets for these elements need to be adjusted
    // depending on whether the window dimensions are even or odd
    var chatMessagesNECorner = document.getElementById("dhChatMessagesNE")
    var chatSendButton = document.getElementById("dhChatSendButton")
    var chatPeopleSWCorner = document.getElementById("dhChatPeopleSW")
        
    if ((width % 2) == 0) {
        chatMessagesNECorner.style.right = "-4px"    
        chatSendButton.style.right = "-1px"          
    } else {
        chatMessagesNECorner.style.right = "-5px"
        chatSendButton.style.right = "-2px"                
    }
  
    if ((height % 2) == 0) {
        chatSendButton.style.bottom = "-1px"         
        chatPeopleSWCorner.style.bottom = "-7px"
    } else {
        chatSendButton.style.bottom = "0px"
        chatPeopleSWCorner.style.bottom = "-6px"               
    }
     
	var bottomHeight = height - (postInfoDiv.offsetHeight + 30)
	var bottomY = postInfoDiv.offsetHeight + 20
    
    var peopleHeight = bottomHeight
    peopleContainer.style.top = bottomY + "px"
    peopleContainer.style.height = peopleHeight + "px"
    peopleDiv.style.top = 10 + "px"
    peopleDiv.style.height = (peopleHeight - 50) + "px"
    peopleDiv.style.width = (peopleContainer.offsetWidth - 6) + "px"

	messagesContainer.style.top = bottomY + "px"
    messagesContainer.style.left = (10 + peopleContainer.offsetWidth) + "px"
    messagesContainer.style.width = ((width - 30) - peopleContainer.offsetWidth) + "px"
    messagesContainer.style.height = peopleContainer.offsetHeight + "px"
    
    messagesDiv.style.width = ((width - 30) - peopleContainer.offsetWidth) + "px"
    messagesDiv.style.height = (bottomHeight - (messageInput.offsetHeight + 40)) + "px"
    messagesDiv.style.paddingLeft = this.MESSAGES_DIV_SIDE_PADDING 
    messagesDiv.style.paddingRight = this.MESSAGES_DIV_SIDE_PADDING 
     
    //_resizeMessage is the callback function that we want to be called for each message
    this._messageList.foreachMessage(function(message, before) { dh.chatwindow._resizeMessage(message, before) })
    // we want to have the old text area width available for resizing, so set the
    // new one only after resizing is done
    this._setTextAreaWidth(this._calculateTextAreaWidth(messagesDiv.offsetWidth))

	messageInput.style.top = messagesDiv.offsetHeight + "px"
    messageInput.style.width = ((width - 30) - peopleContainer.offsetWidth) + "px"
    
    sendButtonDiv.style.top = (messagesDiv.offsetHeight + messageInput.offsetHeight) + "px"
    sendButtonDiv.style.width = ((width - 30) - peopleContainer.offsetWidth) + "px"
    sendButtonDiv.style.height = (peopleHeight - (messagesDiv.offsetHeight + messageInput.offsetHeight)) + "px" 
}

dh.chatwindow.sendClicked = function() {
    var messageInput = document.getElementById("dhChatMessageInput")
    var text = messageInput.value
    text = text.replace(/^\s+/, "")
    text = text.replace(/\s+?$/, "")
    if (text == "") {
        alert("Please enter a non-empty message")
        return;
    }

	dh.control.control.sendChatMessage(this.chatId, text)
    messageInput.value = ""
}

// Note that this handler is used directly and not invoked
// as a method with 'this'
dh.chatwindow.onMessageKeyPress = function() {
	var e = window.event
    if (e.keyCode == 13) {
    	// Suppress event
    	e.returnValue = false
        dh.chatwindow.sendClicked()
    } else if (e.keyCode == 27) {
    	e.returnValue = false
    	window.close();
    }
}

// Note that this handler is used directly and not invoked
// as a method with 'this'
dh.chatwindow.onBodyKeyPress = function () {
	var e = window.event
	if (e.keyCode == 27) {
		e.returnValue = false	
		window.close();
	}
}

// Note that this handler is used directly and not invoked
// as a method with 'this'
dh.chatwindow.onWindowResize = function() {
	dh.chatwindow.resizeElements()
}

dh.chatwindow.setSelfId = function(id) {
	this._selfId = id
}

dh.chatwindow._setTextAreaWidth = function(textAreaWidth) {
    this._textAreaWidth = textAreaWidth
}

dh.chatwindow._calculateTextAreaWidth = function(messageDivWidth) {
    // 37 is for the scroll bar and border
    return messageDivWidth - this.PHOTO_WIDTH*2 - 37
}

dh.chatwindow._createLists = function() {
	this._messageList = new dh.chat.MessageList(
		this._chatRoom,
		function(message, before) { dh.chatwindow._addMessage(message, before) },
		function(message) { dh.chatwindow._removeMessage(message) },
		dh.chatwindow.MAX_HISTORY_COUNT);
		
	this._participantList = new dh.chat.UserList(
		this._chatRoom,
		function(user, before, participant) { dh.chatwindow._addUser(user, before, true) },
		function(user) { dh.chatwindow._removeUser(user) },
		function(user) { dh.chatwindow._updateUser(user) },
		function(user, participant) { return participant });
}

dh.chatwindow.init = function() {
	this._chatRoom = dh.control.control.getOrCreateChatRoom(this.chatId)
	dh.chatwindow._createLists()

	this._chatRoom.join(true)

    var messageInput = document.getElementById("dhChatMessageInput")
    
    // We use this special event handler stuff because there appears
    // to be much more serious leakage when using the Dojo event handler
    // infrastructure (need to investigate Dojo upgrade)
    messageInput.onkeypress = dh.chatwindow.onMessageKeyPress;
    document.body.onkeypress = dh.chatwindow.onBodyKeyPress;

    dh.chatwindow.resizeElements()
    window.onresize = dh.chatwindow.onWindowResize

	messageInput.focus()
}

dh.chatwindow.initDisabled = function() {
    document.body.onkeypress = dh.chatwindow.onBodyKeyPress;
    dojo.event.connect(document.body, "onkeypress", this, "onBodyKeyPress")

    dh.chatwindow.resizeElements()
    window.onresize = dh.chatwindow.onWindowResize
}
