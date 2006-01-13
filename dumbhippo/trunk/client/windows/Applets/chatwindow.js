// Chat room window implementation

dh.chatwindow = {}
dh.display = null

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl) {
    dh.display = new dh.chatwindow.Display(serverUrl, appletUrl) 
    dh.display.init()
}

dh.chatwindow.Display = function(serverUrl, appletUrl) {
    // Base URL of the web server
    this._serverUrl = serverUrl
    
    // URL base for local scripts and pages
    this._appletUrl = appletUrl
    
    this._personDivs = {}
    
    this._createHeadShot = function(userId, version) {
        var url = this._serverUrl + "/files/headshots/48/" + userId + "?v=" + version
        dh.util.debug(url)
        return dh.util.createPngElement(url, 48, 48)
    }
    
    // Add a user to the list of current users
    this.addPerson = function(userId, version, name) {
        var peopleDiv = document.getElementById("dhChatPeopleDiv")

        var personDiv = document.createElement("div")
        personDiv.className = "dh-chat-person"
        
        var image = this._createHeadShot(userId, version)        
        image.className = "dh-chat-person-image"
        personDiv.appendChild(image)

        var nameDiv = document.createElement("div")
        nameDiv.className = "dh-chat-person-name"
        personDiv.appendChild(nameDiv)

        var nameSpan = document.createElement("span")
        nameSpan.appendChild(document.createTextNode(name))
        nameSpan.className = "dh-chat-person-name-inner"
        nameDiv.appendChild(nameSpan)

        this._personDivs[userId] = personDiv
        peopleDiv.appendChild(personDiv)
    }
    
    // Remove the user from the list of current users
    this.removePerson = function(userId) {
        var div = this._personDivs[userId]
        if (div) {
            div.parentNode.removeChild(div)
            delete this._personDivs[userId]
        }
    }

    // Add a message to the message area
    this.addMessage = function(userId, version, name, message) {
        var messagesDiv = document.getElementById("dhChatMessagesDiv")

        var messageDiv = document.createElement("div")
        if (userId == window.external.application.GetSelfId())
            messageDiv.className = "dh-chat-message-my"
        else
            messageDiv.className = "dh-chat-message-other"
        
        var image = this._createHeadShot(userId, version)
        image.className = "dh-chat-message-image"
        messageDiv.appendChild(image)

        var textDiv = document.createElement("div")
        textDiv.className = "dh-chat-message-text"
        messageDiv.appendChild(textDiv)

        var textSpan = document.createElement("span")
        textSpan.appendChild(document.createTextNode(message))
        textSpan.className = "dh-chat-message-text-inner"
        textDiv.appendChild(textSpan)

        messagesDiv.appendChild(messageDiv)
    }
    
    // Clear all messages and users (called on reconnect)
    this.clear = function() {
        var peopleDiv = document.getElementById("dhChatPeopleDiv")
        dh.util.dom.clearNode(peopleDiv)
        
        this._personDivs = {}

        var messagesDiv = document.getElementById("dhChatMessagesDiv")
        dh.util.dom.clearNode(messagesDiv)

    }

    // Adjust element sizes for the current window size; we need to do this
    // manually since some things we want aren't possible with pure CSS,
    // especially with the IE limitations
    this.resizeElements = function() {
        var width, height
        if (window.innerWidth) {
            width = window.innerWidth
            height = window.innerHeight
        } else {
            width = document.body.offsetWidth
            height = document.body.offsetHeight
        }
        
        dh.util.debug("width = " + width + "; height = " + height)
        
        var peopleDiv = document.getElementById("dhChatPeopleDiv")
        var adsDiv = document.getElementById("dhChatAdsDiv")
        var messagesDiv = document.getElementById("dhChatMessagesDiv")
        var messageInput = document.getElementById("dhChatMessageInput")
        var sendButton = document.getElementById("dhChatSendButton")
        
        peopleHeight = (height - 30) / 2
        peopleDiv.style.height = peopleHeight + "px"
        adsDiv.style.top = (peopleHeight + 20) + "px"
        adsDiv.style.height = ((height - 30) - peopleHeight)  + "px"

        messagesDiv.style.left = (20 + adsDiv.offsetWidth) + "px"
        messagesDiv.style.width = ((width - 30) - adsDiv.offsetWidth) + "px"
        messagesDiv.style.height = (height - 30 - messageInput.offsetHeight) + "px"

        messageInput.style.left = (20 + adsDiv.offsetWidth) + "px"
        messageInput.style.width = ((width - 40) - adsDiv.offsetWidth - sendButton.offsetWidth) + "px"
        
        sendButton.style.height = messageInput.offsetHeight + "px"
    }
    
    this.sendClicked = function() {
        var messageInput = document.getElementById("dhChatMessageInput")
        var text = messageInput.value
        text = text.replace(/^\s+/, "")
        text = text.replace(/\s+?$/, "")
        if (text == "") {
            alert("Please enter a non-empty message")
            return;
        }
        window.external.application.SendMessage(text)
        messageInput.value = ""
    }
    
    this.onMessageKeyPress = function(e) {
        if (e.keyCode == 13) {
            this.sendClicked()
            return false
        }
            
        return true
    }
    
    this.init = function() {
        var messageInput = document.getElementById("dhChatMessageInput")
        messageInput.onkeypress = dh.util.dom.stdEventHandler(function(e) { return dh.display.onMessageKeyPress(e) })
    
        dh.display.resizeElements()
        window.onresize = function() { dh.display.resizeElements() }
    }
    
}

function dhChatAddPerson(userId, version, name) {
    dh.display.addPerson(userId, version, name)
}

function dhChatRemovePerson(userId) {
    dh.display.removePerson(userId)
}

function dhChatAddMessage(userId, version, name, text) {
    dh.util.debug("Adding message: " + userId + "/" + version + "/" + name + ": " + text);
    dh.display.addMessage(userId, version, name, text)
}

function dhChatClear() {
    dh.display.clear()
}
