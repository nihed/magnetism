dojo.provide("dh.chatwindow")

dojo.require("dh.chat")
dojo.require("dh.util")
dojo.require("dojo.event")

dh.chatwindow._selfId = null
dh.chatwindow._textAreaWidth = null
dh.chatwindow._photoWidth = 48
dh.chatwindow._messageList = new dh.chat.MessageList(
	function(message, before) { dh.chatwindow._addMessage(message, before) },
	function(message) { dh.chatwindow._removeMessage(message) },
	function(message, before) { dh.chatwindow._resizeMessage(message, before) })
dh.chatwindow._userList = new dh.chat.UserList(
	function(user, before) { dh.chatwindow._addUser(user, before) },
	function(user) { dh.chatwindow._removeUser(user) },
	function(user, arrangementName, artist, musicPlaying) { dh.chatwindow._updateUserMusic(user, arrangementName, artist, musicPlaying) })
    
dh.chatwindow._createHeadShot = function(userId, version) {
    var url = "/files/headshots/" + dh.chatwindow._photoWidth + "/" + userId + "?v=" + version
    return dh.util.createPngElement(url, dh.chatwindow._photoWidth, dh.chatwindow._photoWidth)
}

// Add a user to the list of current users
dh.chatwindow.onUserJoin = function(userId, version, name, participant) {
	if (!participant)
		return;
		
	var user = new dh.chat.User(userId, version, name)
	this._userList.userJoin(user)
}

dh.chatwindow.onUserLeave = function(userId) {
	this._userList.userLeave(userId)
}

// Add a message to the message area
dh.chatwindow.onMessage = function(userId, version, name, text, timestamp, serial) {
	var message = new dh.chat.Message(userId, version, name, text, timestamp, serial)
	this._messageList.addMessage(message)
}

// Clear all messages and users (called on reconnect)
dh.chatwindow.onReconnect = function() {
	this._messageList.clear()
	this._userList.clear()
}

// Update music for a given user
dh.chatwindow.onUserMusicChange = function(userId, arrangementName, artist, musicPlaying) {
	this._userList.userMusicChange(userId, arrangementName, artist, musicPlaying)
}


// Check if we are scrolled to the bottom (with 8 pixels of fuzz)
dh.chatwindow._isAtBottom = function(element) {
	return element.scrollTop >= element.scrollHeight - element.clientHeight - 8
}

// Scroll to the bottom
dh.chatwindow._scrollToBottom = function(element) {
	element.scrollTop = element.scrollHeight - element.clientHeight
}

dh.chatwindow._addMessage = function(message, before) {
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
    
    var image = this._createHeadShot(message.userId, message.version)
    image.className = "dh-chat-message-image"
    image.title = message.name
	image.style.width = this._photoWidth + "px";
    message.div.appendChild(image)

    var messagesDiv = document.getElementById("dhChatMessagesDiv")      
	var wasAtBottom = this._isAtBottom(messagesDiv)
    messagesDiv.insertBefore(message.div, before ? before.div : null)
		
    var textDiv = document.createElement("div")
    textDiv.className = "dh-chat-message-text"
    var textSidePadding = 10		
    var textPadding = 2
    
    var textWidth = dh.util.getTextWidth(message.text) + textSidePadding*2
 
    // 30 is for the scroll bar and such
    // here we are interested in the new textAreaWidth
    var textAreaWidth = messagesDiv.offsetWidth - this._photoWidth*2 - 30
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
		
    if (message.userId == this._selfId) {   
        if (!message.userFirst) {
            message.div.style.marginLeft = this._photoWidth + "px"
            // 3px are added in some unknown way to the texDivs that
            // inline next to the photo, this is how we can distinguish
            // if the texDiv inlined past the photo, and make sure
            // all messages are aligned 
            if (textDiv.offsetLeft == this._photoWidth) {
               message.div.style.marginLeft = this._photoWidth + 3 + "px"               
            }
        }
    }
    else {
        message.div.style.marginLeft = (textAreaWidth - textWidth) + this._photoWidth + 3 + "px"      	
    }
	
	// if this is not a first message in a given batch of messages, make sure that they all are
	// the same width, which should be the width of the longest message in this batch
	if (!message.userFirst) {
	    var doneWithPrevSiblings = 0
	    var prevSibling = message.div.previousSibling
	    while ((doneWithPrevSiblings == 0) && (prevSibling)) {	     	    
	        // find a previous sibling, compare sizes
            // get to the class attribute
            var i = 0
            var classAttributeFound = 0
            while ((classAttributeFound == 0) && (i < prevSibling.attributes.length)) {
                if (prevSibling.attributes.item(i).specified) {
			        if (prevSibling.attributes.item(i).nodeName.toLowerCase() == "class") {
			            classAttributeFound = 1
			            if (prevSibling.lastChild.offsetWidth < textWidth) {  
			                // the width of a new message is going to be longer than the width
			                // of previous messages in this batch   
			                // in this case we need to go through all the previous siblings 
			                // and update them, which is why we are not going to set 
			                // doneWithPrevSiblings to true here	            
			                prevSibling.lastChild.style.width = textDiv.style.width
			                if (message.userId != this._selfId) {
			                    prevSibling.style.marginLeft = message.div.style.marginLeft
			                }	
			            } else {
			                // the width of a new message is shorter or the same as the width
			                // of previous messages, so we should make sure it is the same 
			                textDiv.style.width = prevSibling.lastChild.style.width		            			            
			                if (message.userId != this._selfId) {
			                    message.div.style.marginLeft = prevSibling.style.marginLeft
			                }
			                doneWithPrevSiblings = 1 
			            }	    			                
                        var classStr = prevSibling.attributes.item(i).nodeValue
			            if (classStr.indexOf("dh-chat-message-user-first") >= 0) {
			                doneWithPrevSiblings = 1    
			            }
                    }
                }    
                i++
            }
            if (!classAttributeFound) {
                // we should not have a sibling without a class attribute here
                var error = "found a sibling of a message div missing a class attribute"
				dojo.debug(error)
				alert(error)
			    doneWithPrevSiblings = 1                             
            }
            prevSibling = prevSibling.previousSibling  
        }    
	}	 
		
    var textSpan = document.createElement("span")		
    var textNode = document.createTextNode(message.text)
    textSpan.appendChild(textNode)
    textSpan.className = "dh-chat-message-text-inner"
        
    textDiv.appendChild(textSpan)
		
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
    var newTextAreaWidth = dh.chatwindow.calculateTextAreaWidth(messagesDiv.offsetWidth)
    // last child of a message div is the textDiv
    var currentTextWidth = message.div.lastChild.offsetWidth
    if ((currentTextWidth >= oldTextAreaWidth) || (currentTextWidth > newTextAreaWidth)) {
        dh.chatwindow._removeMessage(message)
        dh.chatwindow._addMessage(message, before)
    } else if (message.userId != this._selfId) {   
        // we are happy with the curent text width, but in the case of messages from other people 
        // that "float" right, we need to readjust the left margin based on the new textAreaWidth 
        message.div.style.marginLeft = (newTextAreaWidth - currentTextWidth) + this._photoWidth + 3 + "px"      	
    }   
}

dh.chatwindow._addUser = function(user, before) {
    user.div = document.createElement("div")
    user.div.className = "dh-chat-person"

    var image = this._createHeadShot(user.userId, user.version)        
    image.className = "dh-chat-person-image"
    user.div.appendChild(image)

    var nameDiv = document.createElement("div")
    nameDiv.className = "dh-chat-person-name"
    user.div.appendChild(nameDiv)

    var nameSpan = document.createElement("span")
    nameSpan.appendChild(document.createTextNode(user.name))
    nameSpan.className = "dh-chat-person-name-inner"
    nameDiv.appendChild(nameSpan)

    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    peopleDiv.insertBefore(user.div, before ? before.div : null) 
}

dh.chatwindow._removeUser = function(user) {
    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    peopleDiv.removeChild(user.div)
}

dh.chatwindow._updateUserMusic = function(user, arrangementName, artist, musicPlaying) {
    newUserDiv = user.div
    var oldArrangementNameDiv
    var oldArtistDiv
    // if musicPlaying flag is false, and arrangementName and artist are not specified, 
    // it means that the old music selection was stopped
    var useOldMusicInfo = ((arrangementName == "") && (artist == "") && !musicPlaying)
    if (!musicPlaying) {
        newUserDiv.style.backgroundImage = "url(/images/personAreaMusicStopped.jpg)"    
    } else {
        newUserDiv.style.backgroundImage = "url(/images/personArea.jpg)"
    }
    
    // find the old arrangementNameDiv and old artistDiv
    for(var i = 0; i < newUserDiv.children.length; i++) {
        var child = newUserDiv.children[i]
        var j = 0
        var classAttributeFound = 0
        while ((classAttributeFound == 0) && (j < child.attributes.length)) {
            if (child.attributes.item(j).specified) {
                if (child.attributes.item(j).nodeName.toLowerCase() == "class") {			                      
                    var classStr = child.attributes.item(j).nodeValue
			        if (classStr.indexOf("dh-chat-person-arrangement-name") >= 0) {
			            oldArrangementNameDiv = child    
			        }
			        if (classStr.indexOf("dh-chat-person-artist") >= 0) {
			            oldArtistDiv = child    
			        }			            
                }
            }    
            j++
        }
    }

    if (useOldMusicInfo) {
        if (oldArrangementNameDiv) {    
            if (oldArrangementNameDiv.className.indexOf("dh-chat-person-music-stopped") < 0) {
                oldArrangementNameDiv.className += " dh-chat-person-music-stopped"
            }
        }
        if (oldArtistDiv) {
            if (oldArtistDiv.className.indexOf("dh-chat-person-music-stopped") < 0) {
                oldArtistDiv.className += " dh-chat-person-music-stopped"
            }
        }        
        return;
    } 

    var arrangementNameDiv = document.createElement("div")
    arrangementNameDiv.className = "dh-chat-person-arrangement-name"
    if (!musicPlaying) {
        arrangementNameDiv.className += " dh-chat-person-music-stopped"
    }
    
    if (oldArrangementNameDiv) {
        newUserDiv.replaceChild(arrangementNameDiv, oldArrangementNameDiv)
    } else {
        newUserDiv.appendChild(arrangementNameDiv)
    }
  
    var arrangementNameSpan = document.createElement("span")
    arrangementNameSpan.appendChild(document.createTextNode(arrangementName))
    arrangementNameSpan.className = "dh-chat-person-arrangement-name-inner"
    arrangementNameDiv.appendChild(arrangementNameSpan)    
  
    var artistDiv = document.createElement("div")
    artistDiv.className = "dh-chat-person-artist"
    if (!musicPlaying) {
        artistDiv.className += " dh-chat-person-music-stopped"
    }
    
    if (oldArtistDiv) {
        newUserDiv.replaceChild(artistDiv, oldArtistDiv)
    } else {
        newUserDiv.appendChild(artistDiv)
    }

    var artistSpan = document.createElement("span")
    artistSpan.appendChild(document.createTextNode(artist))
    artistSpan.className = "dh-chat-person-artist-inner"
    artistDiv.appendChild(artistSpan)        
    
    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    peopleDiv.replaceChild(newUserDiv, user.div) 
}

// Adjust element sizes for the current window size; we need to do this
// manually since some things we want aren't possible with pure CSS,
// especially with the IE limitations
dh.chatwindow.resizeElements = function() {
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
    var adsDiv = document.getElementById("dhChatAdsDiv")
    var messagesDiv = document.getElementById("dhChatMessagesDiv")
    var messageInput = document.getElementById("dhChatMessageInput")
    var sendButton = document.getElementById("dhChatSendButton")
    
	var bottomHeight = height - (postInfoDiv.offsetHeight + 30)
	var bottomY = postInfoDiv.offsetHeight + 20
    
    var peopleHeight = 3*(bottomHeight/4)
    peopleContainer.style.top = bottomY + "px"
    peopleContainer.style.height = peopleHeight + "px"
    peopleDiv.style.top = 17 + "px";
    peopleDiv.style.height = (peopleHeight - 17) + "px"
    adsDiv.style.top = (bottomY + peopleHeight) + "px"
    adsDiv.style.height = (bottomHeight - peopleHeight) + "px"
    adsDiv.style.width = (width - 60) + "px"

	messagesDiv.style.top = bottomY + "px"
    messagesDiv.style.left = (10 + peopleContainer.offsetWidth) + "px"
    messagesDiv.style.width = ((width - 30) - peopleContainer.offsetWidth) + "px"
    messagesDiv.style.height = (bottomHeight - (messageInput.offsetHeight + 10)) + "px"
    
    this._messageList.resizeMessages()  
    // we want to have the old text area width available for resizing, so set the
    // new one only after resizing is done
    dh.chatwindow.setTextAreaWidth(dh.chatwindow.calculateTextAreaWidth(messagesDiv.offsetWidth))

	messageInput.style.top = (bottomY + messagesDiv.offsetHeight) + "px"
    messageInput.style.left = (10 + peopleContainer.offsetWidth) + "px"
    messageInput.style.width = ((width - 30) - peopleContainer.offsetWidth - sendButton.offsetWidth - 2) + "px"
    
    sendButton.style.left = (10 + peopleContainer.offsetWidth + messageInput.offsetWidth) + "px"
    sendButton.style.top = (bottomY + messagesDiv.offsetHeight) + "px"
    sendButton.style.height = messageInput.offsetHeight + "px"
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

dh.chatwindow.onMessageKeyPress = function(e) {
    if (e.keyCode == 13) {
        this.sendClicked()
        e.preventDefault()
    }
}

dh.chatwindow.setSelfId = function(id) {
	this._selfId = id
}

dh.chatwindow.setTextAreaWidth = function(textAreaWidth) {
    this._textAreaWidth = textAreaWidth
}

dh.chatwindow.calculateTextAreaWidth = function(messageDivWidth) {
    return messageDivWidth - this._photoWidth*2 - 30
}

dh.chatwindow.init = function() {
	var chatControl = document.getElementById("dhChatControl")

    var messageInput = document.getElementById("dhChatMessageInput")
    dojo.event.connect(messageInput, "onkeypress", this, "onMessageKeyPress")

    dh.chatwindow.resizeElements()
    window.onresize = function() { dh.chatwindow.resizeElements() }

	messageInput.focus()
}

dh.chatwindow.rescan = function() {
	var chatControl = document.getElementById("dhChatControl")
    if (chatControl && chatControl.readyState && chatControl.readyState == 4) {
		chatControl.Rescan()
	}
}
