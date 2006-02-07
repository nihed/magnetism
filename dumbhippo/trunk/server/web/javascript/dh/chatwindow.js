dojo.provide("dh.chatwindow")

dojo.require("dh.chat")
dojo.require("dh.util")
dojo.require("dojo.event")

dh.chatwindow._selfId = null
dh.chatwindow._messageList = new dh.chat.MessageList(
	function(message, before) { dh.chatwindow._addMessage(message, before) },
	function(message) { dh.chatwindow._removeMessage(message) })
dh.chatwindow._userList = new dh.chat.UserList(
	function(user, before) { dh.chatwindow._addUser(user, before) },
	function(user) { dh.chatwindow._removeUser(user) })
    
dh.chatwindow._createHeadShot = function(userId, version) {
    var url = "/files/headshots/48/" + userId + "?v=" + version
    return dh.util.createPngElement(url, 48, 48)
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
    message.div.appendChild(image)

    var textDiv = document.createElement("div")
    textDiv.className = "dh-chat-message-text"
    message.div.appendChild(textDiv)

    var textSpan = document.createElement("span")
    textSpan.appendChild(document.createTextNode(message.text))
    textSpan.className = "dh-chat-message-text-inner"
    textDiv.appendChild(textSpan)

    var messagesDiv = document.getElementById("dhChatMessagesDiv")

	var wasAtBottom = this._isAtBottom(messagesDiv)
    messagesDiv.insertBefore(message.div, before ? before.div : null)
	if (!before && wasAtBottom)
		this._scrollToBottom(messagesDiv)
}

dh.chatwindow._removeMessage = function(message) {
    var messagesDiv = document.getElementById("dhChatMessagesDiv")
	messagesDiv.removeChild(message.div)
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
    var peopleDiv = document.getElementById("dhChatPeopleDiv")
    var adsDiv = document.getElementById("dhChatAdsDiv")
    var messagesDiv = document.getElementById("dhChatMessagesDiv")
    var messageInput = document.getElementById("dhChatMessageInput")
    var sendButton = document.getElementById("dhChatSendButton")
    
	var bottomHeight = height - (postInfoDiv.offsetHeight + 30)
	var bottomY = postInfoDiv.offsetHeight + 20
    
    var peopleHeight = ((bottomHeight - 10) / 2)
    peopleDiv.style.top = bottomY + "px"
    peopleDiv.style.height = peopleHeight + "px"
    adsDiv.style.top = (bottomY + peopleHeight + 10) + "px"
    adsDiv.style.height = (bottomHeight - (peopleHeight + 10))  + "px"

	messagesDiv.style.top = bottomY + "px"
    messagesDiv.style.left = (20 + adsDiv.offsetWidth) + "px"
    messagesDiv.style.width = ((width - 30) - adsDiv.offsetWidth) + "px"
    messagesDiv.style.height = (bottomHeight - (messageInput.offsetHeight + 10)) + "px"

    messageInput.style.left = (20 + adsDiv.offsetWidth) + "px"
    messageInput.style.width = ((width - 40) - adsDiv.offsetWidth - sendButton.offsetWidth) + "px"
    
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

dh.chatwindow.init = function() {
	var chatControl = document.getElementById("dhChatControl")

    var messageInput = document.getElementById("dhChatMessageInput")
    dojo.event.connect(messageInput, "onkeypress", this, "onMessageKeyPress")

    dh.chatwindow.resizeElements()
    window.onresize = function() { dh.chatwindow.resizeElements() }

	messageInput.focus()
}
