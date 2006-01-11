// Chat room window implementation

dh.chatroom = {}
dh.display = null

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl, selfId) {
    dh.display = new dh.chatroom.Display(serverUrl, appletUrl, selfId) 
    
    dh.display.resizeElements()
    window.onresize = function() { dh.display.resizeElements() }
    
    dh.display.addTestMessages()
}

dh.chatroom.Display = function(serverUrl, appletUrl, selfId) {
    // Current user guid
    this._selfId = selfId

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
        if (userId == this._selfId)
            return // Don't show the user themselves
        
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
    this.removePerson = function() {
        var div = this._personDivs[userId]
        if (div) {
            div.parentNode.removeChild(div)
            delete this._personDivs[userId]
        }
    }

    // Add a message to the message area
    this.addMessage = function(userId, version, message) {
        var messagesDiv = document.getElementById("dhChatMessagesDiv")

        var messageDiv = document.createElement("div")
        if (userId == this._selfId)
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
    
    this.addTestMessages = function() {
        this.addPerson("hKcbRMYl4vNDqw", 2, "Colin")
        for (var i = 0; i < 10; i++) {
            this.addMessage("hKcbRMYl4vNDqw", 2, "Foo, Bar, Baz, Boof, Boof, Boof")
            this.addMessage(this._selfId, 28, "Bah, Nah, Nah, Nah")
        }
    }
}

function dhChatAddPerson(userId, version, name) {
    dh.display.addPerson(userId, version, name)
}

function dhChatRemovePerson(userId) {
    dh.display.removePerson(userId)
}

function dhChatAddMessage(userId, version, text) {
    dh.display.addMessage(usreId, version, text)
}
