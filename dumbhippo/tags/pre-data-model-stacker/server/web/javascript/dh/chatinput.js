dojo.provide("dh.chatinput");

dojo.require("dh.control");
dojo.require("dh.dom");
dojo.require("dh.event");
dojo.require("dh.util");

dh.chatinput._sentimentMap = {
	dhChatIndifferent: dh.control.SENTIMENT_INDIFFERENT,
	dhChatLove:        dh.control.SENTIMENT_LOVE,
	dhChatHate:        dh.control.SENTIMENT_HATE
};

dh.chatinput.setSentiment = function(sentiment) {
	this._sentiment = sentiment;

	for (var id in this._sentimentMap) {
		var span = document.getElementById(id);
		if (sentiment == this._sentimentMap[id]) {
			span.className = "dh-chat-sentiment dh-chat-sentiment-selected";
		} else {
			span.className = "dh-chat-sentiment";
		}
	}
}

dh.chatinput.getSentiment = function() {
	return this._sentiment;
}

dh.chatinput.getText = function() {
    var messageInput = document.getElementById("dhChatMessageInput");
    return dh.util.trim(messageInput.value);
}

dh.chatinput.setText = function(text) {
    var messageInput = document.getElementById("dhChatMessageInput");
	messageInput.value = text;
}

dh.chatinput.focus = function() {
    var messageInput = document.getElementById("dhChatMessageInput");
	messageInput.focus();
}

// Hook to customize the effect of hitting escape
dh.chatinput.onCancel = function() {
}

// Called as an event handler, and not as a method with 'this'
dh.chatinput._onSentimentClick = function(e) {
	var node = dh.event.getNode(e);
	var sentiment = dh.chatinput._sentimentMap[node.id];
	dh.chatinput.setSentiment(sentiment);

	dh.event.cancel(e);
	
	// Send the focus back to the input box
    var messageInput = document.getElementById("dhChatMessageInput");
    messageInput.focus();
	
	return false;
}

dh.chatinput._noopEvent = function(e) {
	dh.event.cancel(e);
	return false;
}

dh.chatinput._preventSelection = function(node) {
    dh.event.addEventListener(node, "mousedown",
							  this._noopEvent);
    dh.event.addEventListener(node, "move",
							  this._noopEvent);
	
	for (var i = 0; i < node.childNodes.length; i++) {
		if (node.childNodes[i].nodeType == dh.dom.ELEMENT_NODE)
			this._preventSelection(node.childNodes[i]);
	}
}

// Note that this handler is used directly and not invoked
// as a method with 'this'
dh.chatinput._onMessageKeyPress = function(e) {
	var keycode = dh.event.getKeyCode(e);
    if (keycode == 13) {
    	dh.event.cancel(e);
    	
    	var addButton = document.getElementById("dhChatAddButton");
    	if (!addButton.disabled)
	    	addButton.onclick();
    	
        return false;
    } else if (keycode == 27) {
    	dh.event.cancel(e);
    	dh.chatinput.onCancel();
    	return false;
    }
	
	return true;
}

dh.chatinput.init = function() {
    var messageInput = document.getElementById("dhChatMessageInput")
    
    dh.event.addEventListener(messageInput, "keypress",
							  dh.chatinput._onMessageKeyPress);

	for (var id in this._sentimentMap) {
		var span = document.getElementById(id);
		this._preventSelection(span);
    	dh.event.addEventListener(span, "click",
						    	  this._onSentimentClick);
	}
	
	this.setSentiment(dh.control.SENTIMENT_INDIFFERENT);
}