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

dh.chatwindow._messageList = new dh.chat.MessageList(
	function(message, before) { dh.chatwindow._addMessage(message, before) },
	function(message) { dh.chatwindow._removeMessage(message) },
	dh.chatwindow.MAX_HISTORY_COUNT)
dh.chatwindow._userList = new dh.chat.UserList(
	function(user, before) { dh.chatwindow._addUser(user, before) },
	function(user) { dh.chatwindow._removeUser(user) })
    
dh.chatwindow._createHeadShot = function(photoUrl) {
    return dh.util.createPngElement(photoUrl, dh.chatwindow.PHOTO_WIDTH, dh.chatwindow.PHOTO_WIDTH)
}

// Add a user to the list of current users
dh.chatwindow.onUserJoin = function(userId, photoUrl, name, participant) {
	if (!participant)
		return;
	
	var user = new dh.chat.User(userId, photoUrl, name)
	this._userList.userJoin(user)
}

dh.chatwindow.onUserLeave = function(userId) {
	this._userList.userLeave(userId)
}

// Add a message to the message area
dh.chatwindow.onMessage = function(userId, photoUrl, name, text, timestamp, serial) {
	var message = new dh.chat.Message(userId, photoUrl, name, text, timestamp, serial)
	this._messageList.addMessage(message)
}

// Clear all messages and users (called on reconnect)
dh.chatwindow.onReconnect = function() {
	this._messageList.clear()
	this._userList.clear()
}

// Update music for a given user
dh.chatwindow.onUserMusicChange = function(userId, arrangementName, artist, musicPlaying) {
    var user = this._userList.getUser(userId)
	// a user might not be in the _userList if he wasn't yet reported as a participant
    if (user) {  
	    dh.chatwindow._updateUserMusic(user, arrangementName, artist, musicPlaying)
	} 
}

// Check if we are scrolled to the bottom (with 8 pixels of fuzz)
dh.chatwindow._isAtBottom = function(element) {
	return element.scrollTop >= element.scrollHeight - element.clientHeight - 8
}

// Scroll to the bottom
dh.chatwindow._scrollToBottom = function(element) {
	element.scrollTop = element.scrollHeight - element.clientHeight
}

dh.chatwindow._addMessage = function(message, before, resizingFlag) {
	dh.util.clientDebug("adding chat message: " + message + " resizing: " + resizingFlag)
	message.div = document.createElement("div")
	message.div.className = "dh-chat-message"
    if (message.userId == this._selfId)
        message.div.className += " dh-chat-message-my"
    else
        message.div.className += " dh-chat-message-other"
    if (message.userFirst)
        message.div.className += " dh-chat-message-user-first"
	else
        message.div.className += " dh-chat-message-user-repeat"
    
    var image = this._createHeadShot(message.photoUrl)
    image.className = "dh-chat-message-image"
    image.title = message.name
    var userUrl = "/person?who=" + message.userId
    var linkElement = dh.util.createLinkElementWithChild(userUrl, image)       
    message.div.appendChild(linkElement)

    var messagesDiv = document.getElementById("dhChatMessagesDiv")      
	var wasAtBottom = this._isAtBottom(messagesDiv)
    messagesDiv.insertBefore(message.div, before ? before.div : null)
		
    var textDiv = document.createElement("div")
    textDiv.className = "dh-chat-message-text"

    var messageFontStyle = dh.chat.getMessageFontStyle(message)
    var textSidePadding = 8
    var textPadding = 2
    
    var textWidth = dh.util.getTextWidth(message.text, this.MESSAGES_FONT_FAMILY, null, messageFontStyle) + textSidePadding*2
 
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
         
    message.div.appendChild(textDiv)         
    var textSpan = document.createElement("span")		
    dh.util.insertTextWithLinks(textSpan, message.text)       
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
		
    if (message.userId == this._selfId) {   
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
            message.div.style.marginLeft = this.PHOTO_WIDTH + "px"         
            
            if (textDiv.offsetLeft == this.PHOTO_WIDTH + this.MESSAGES_DIV_SIDE_PADDING) {
               message.div.style.marginLeft = this.PHOTO_WIDTH + this.FLOAT_ADJUSTMENT  + "px"               
            }
        }
    }
    else {
        message.div.style.marginLeft = (textAreaWidth - textWidth) + this.PHOTO_WIDTH + this.FLOAT_ADJUSTMENT + "px" 
    }
	
	// if this is not a first message in a given batch of messages, make sure that they all are
	// the same width, which should be the width of the longest message in this batch
	if (!message.userFirst) {
	    var doneWithPrevSiblings = false
	    var prevSibling = message.div.previousSibling
	    // compare sizes with previus siblings 
	    while (!doneWithPrevSiblings && prevSibling) {	     	    
            if (prevSibling.lastChild.offsetWidth < textWidth) {  
			    // the width of a new message is going to be longer than the width
                // of previous messages in this batch   
                // in this case we need to go through all the previous siblings 
                // and update them, which is why we are not going to set 
                // doneWithPrevSiblings to true here	            
                prevSibling.lastChild.style.width = textDiv.offsetWidth
                if (message.userId != this._selfId) {
                    prevSibling.style.marginLeft = message.div.style.marginLeft
                }	
            } else {
                // the width of a new message is shorter or the same as the width
                // of previous messages, so we should make sure it is the same 
                textDiv.style.width = prevSibling.lastChild.offsetWidth		
                if (message.userId != this._selfId) {
                    message.div.style.marginLeft = prevSibling.style.marginLeft
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
	if (!resizingFlag && before && (before.userId == message.userId)) {
		var doneWithNextSiblings = false
	    var nextSibling = message.div.nextSibling
	    // compare sizes with next siblings 
	    while (!doneWithNextSiblings && nextSibling) {	     	    
            if (nextSibling.lastChild.offsetWidth < textWidth) {  
			    // the width of a new message is going to be longer than the width
                // of next messages in this batch   
                // in this case we need to go through all the next siblings 
                // and update them, which is why we are not going to set 
                // doneWithNextSiblings to true here	            
                nextSibling.lastChild.style.width = textDiv.offsetWidth
                if (message.userId != this._selfId) {
                    nextSibling.style.marginLeft = message.div.style.marginLeft
                }	
            } else {
                // the width of a new message is shorter or the same as the width
                // of next messages, so we should make sure it is the same 
                textDiv.style.width = nextSibling.lastChild.offsetWidth		            			            
                if (message.userId != this._selfId) {
                    message.div.style.marginLeft = nextSibling.style.marginLeft
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
				
    window.external.application.DemandAttention()
}

dh.chatwindow._removeMessage = function(message) {
    var messagesDiv = document.getElementById("dhChatMessagesDiv")
	messagesDiv.removeChild(message.div)
}

dh.chatwindow._resizeMessage = function(message, before) {
    var messagesDiv = document.getElementById("dhChatMessagesDiv")   
    var messageInput = document.getElementById("dhChatMessageInput")
    var oldTextAreaWidth = this._textAreaWidth
    var newTextAreaWidth = this._calculateTextAreaWidth(messagesDiv.offsetWidth)
    // last child of a message div is the textDiv
    var oldTextWidth = message.div.lastChild.offsetWidth
    // if the message was wrapped before (oldTextWidth is not less than oldTextAreaWidth) 
    // or it wasn't wrapped before but exceeds the new width (oldTextWidth is greater than 
    // newTextAreaWidth), then changing the width of the window should change the width of
    // the message, so we need to recalculate its size
    if ((oldTextWidth >= oldTextAreaWidth) || (oldTextWidth > newTextAreaWidth)) {
        dh.chatwindow._removeMessage(message)
        dh.chatwindow._addMessage(message, before, true)
    } else if (message.userId != this._selfId) {   
        // we are happy with the curent text width, but in the case of messages from other people 
        // that "float" right, we need to readjust the left margin based on the new textAreaWidth 
        message.div.style.marginLeft = (newTextAreaWidth - oldTextWidth) + this.PHOTO_WIDTH + this.FLOAT_ADJUSTMENT + "px"      	
    }   
}

dh.chatwindow._addUser = function(user, before) {
    user.div = document.createElement("div")
    user.div.className = "dh-chat-person"

    var userUrl = "/person?who=" + user.userId
    
    var image = this._createHeadShot(user.photoUrl)        
    image.className = "dh-chat-person-image"
    image.title = user.name
    var linkElement = dh.util.createLinkElementWithChild(userUrl, image)
    user.div.appendChild(linkElement)

    var nameDiv = document.createElement("div")
    nameDiv.className = "dh-chat-person-name"
    nameDiv.title = user.name
    user.div.appendChild(nameDiv)

    var nameSpan = document.createElement("span")
    nameSpan.className = "dh-chat-person-name-inner"
    dh.util.addLinkElement(nameSpan, userUrl, user.name)
    nameDiv.appendChild(nameSpan)

    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    peopleDiv.insertBefore(user.div, before ? before.div : null) 
}

dh.chatwindow._removeUser = function(user) {
    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    peopleDiv.removeChild(user.div)
}

dh.chatwindow._updateUserMusic = function(user, arrangementName, artist, musicPlaying) {
    var userDiv = user.div
    var arrangementNameDiv = user.arrangementNameDiv
    var artistDiv = user.artistDiv
    // if musicPlaying flag is false, and arrangementName and artist are not specified, 
    // it means that the old music selection was stopped
    var useOldMusicInfo = ((arrangementName == "") && (artist == "") && !musicPlaying)
    if (!musicPlaying) {
        userDiv.style.backgroundImage = "url(/images2/personAreaNoteOff_60.gif)"    
    } else {
        userDiv.style.backgroundImage = "url(/images2/personAreaNoteOn_60.gif)"
    }
   
    if (useOldMusicInfo) {
        if (arrangementNameDiv) {    
            if (arrangementNameDiv.className.indexOf("dh-chat-person-music-stopped") < 0) {
                arrangementNameDiv.className += " dh-chat-person-music-stopped"
            }
        }
        if (artistDiv) {
            if (artistDiv.className.indexOf("dh-chat-person-music-stopped") < 0) {
                artistDiv.className += " dh-chat-person-music-stopped"
            }
        }        
        return;
    } 

    var arrangementLinkUrl = "/artist?track=" + arrangementName + "&artist=" + artist 
             
    if (!arrangementNameDiv) {
        arrangementNameDiv = document.createElement("div")
        var arrangementNameSpan = document.createElement("span")
        arrangementNameSpan.className = "dh-chat-person-arrangement-name-inner"
        dh.util.addLinkElement(arrangementNameSpan, arrangementLinkUrl, arrangementName)
        arrangementNameDiv.appendChild(arrangementNameSpan)    
        userDiv.appendChild(arrangementNameDiv)
        user.arrangementNameDiv = arrangementNameDiv        
    } else {
        var arrangementLink = dh.util.createLinkElement(arrangementLinkUrl, arrangementName)        
        arrangementNameDiv.firstChild.replaceChild(arrangementLink, arrangementNameDiv.firstChild.firstChild)
    }
    
    // it is easier to reset the class for the arrangementNameDiv completely, 
    // than to change the old one
    arrangementNameDiv.className = "dh-chat-person-arrangement-name"
    if (!musicPlaying) {
        arrangementNameDiv.className += " dh-chat-person-music-stopped"
    }    
    arrangementNameDiv.title = arrangementName
         
    if (!artistDiv) {
        artistDiv = document.createElement("div")
        var artistSpan = document.createElement("span")
        artistSpan.className = "dh-chat-person-artist-inner"        
        dh.util.addLinkElement(artistSpan, arrangementLinkUrl, artist)
        artistDiv.appendChild(artistSpan)           
        userDiv.appendChild(artistDiv)
        user.artistDiv = artistDiv        
    } else {
        var artistLink = dh.util.createLinkElement(arrangementLinkUrl, artist)    
        artistDiv.firstChild.replaceChild(artistLink, artistDiv.firstChild.firstChild)
    }
    
    // same as above, it is easier to reset the class for the artistDiv completely, 
    // than to change the old one
    artistDiv.className = "dh-chat-person-artist"
    if (!musicPlaying) {
        artistDiv.className += " dh-chat-person-music-stopped"
    }
    artistDiv.title = artist
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

	var chatControl = document.getElementById("dhChatControl")
    chatControl.SendMessage(text)
    messageInput.value = ""
}

dh.chatwindow.onMessageKeyPress = function() {
	var e = window.event
    if (e.keyCode == 13) {
    	// Suppress event
    	e.returnValue = false
        this.sendClicked()
    } else if (e.keyCode == 27) {
    	e.returnValue = false
    	window.close();
    }
}

dh.chatwindow.onBodyKeyPress = function () {
	var e = window.event
	if (e.keyCode == 27) {
		e.returnValue = false	
		window.close();
	}
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

dh.chatwindow.init = function() {
	var chatControl = document.getElementById("dhChatControl")

    var messageInput = document.getElementById("dhChatMessageInput")
    var chatWindow = this
    // We use this special event handler stuff because there appears
    // to be much more serious leakage when using the Dojo event handler
    // infrastructure (need to investigate Dojo upgrade)
    messageInput.onkeypress = function () { chatWindow.onMessageKeyPress(); }
    document.body.onkeypress = function () { chatWindow.onBodyKeyPress(); }

    dh.chatwindow.resizeElements()
    window.onresize = function() { dh.chatwindow.resizeElements() }

	messageInput.focus()
}

dh.chatwindow.initDisabled = function() {
	var chatControl = document.getElementById("dhChatControl")


    dojo.event.connect(document.body, "onkeypress", this, "onBodyKeyPress")

    dh.chatwindow.resizeElements()
    window.onresize = function() { dh.chatwindow.resizeElements() }
}

dh.chatwindow.rescan = function() {
	var chatControl = document.getElementById("dhChatControl")
    if (chatControl && chatControl.readyState && chatControl.readyState == 4) {
		chatControl.Rescan()
	}
}
