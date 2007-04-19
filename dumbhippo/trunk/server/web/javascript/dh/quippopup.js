dojo.provide("dh.quippopup");

dojo.require("dh.chatinput");
dojo.require("dh.util");

// Set from quipPopup.tag
dh.quippopup.selfName = null;
dh.quippopup.selfHomeUrl = null;

dh.quippopup._chatId = null;
dh.quippopup._onComplete = null;

dh.quippopup.send = function() {
    var text = dh.chatinput.getText();
    if (text == "") {
        alert("Please enter a non-empty message");
        return;
    }
    
    var sentiment = dh.chatinput.getSentiment();
    var sentimentString;
    if (sentiment == dh.control.SENTIMENT_INDIFFERENT)
    	sentimentString = "INDIFFERENT";
    else if (sentiment == dh.control.SENTIMENT_LOVE)
    	sentimentString = "LOVE";
    else if (sentiment == dh.control.SENTIMENT_HATE)
    	sentimentString = "HATE";

    dh.server.doXmlMethod("addChatMessage",
	                      { "chatId" : this._chatId,
	                        "text" : text,
	                        "sentiment" : sentimentString },
	                      function() {
					          dh.chatinput.setText("");
                         	  dh.quippopup._end();
                         	  if (dh.quippopup._onComplete != null) {
                         	      dh.quippopup._onComplete(dh.quippopup.selfName, dh.quippopup.selfHomeUrl, text, sentiment);
                         	  }
	                      },
	                      function(code, msg) {
	                       	  alert("Unable to add the quip: ", msg);
	                      });
}

dh.quippopup.start = function(chatId, title, x, y, sentiment, onComplete) {
	this._chatId = chatId;
	this._onComplete = onComplete;

	var quipPopup = document.getElementById("dhQuipPopup");
	
	var quipPopupTitle = document.getElementById("dhQuipPopupTitle");
	dh.util.clearNode(quipPopupTitle);
	if (title != null)
		quipPopupTitle.appendChild(document.createTextNode(title));
	
	dh.chatinput.setSentiment(sentiment);
	dh.chatinput.setText("");
	
	quipPopup.style.visibility = "hidden";
	quipPopup.style.display = "block";
	
	x = x + 30 - quipPopup.offsetWidth;
	y = y - 5;
	
	var pageWidth = window.innerWidth ? window.innerWidth : document.body.offsetWidth;

	if (x + quipPopup.offsetWidth > pageWidth - 5)
		x = pageWidth - 5 - quipPopup.offsetWidth;
	if (x < 5)
		x = 5;
	if (y < 5)
		y = 5;

	var parentPos = dh.util.getBodyPosition(quipPopup.parentNode);

	quipPopup.style.left = (x - parentPos.x) + "px";
	quipPopup.style.top = (y - parentPos.y) + "px";
	
	quipPopup.style.visibility = "visible";

	dh.chatinput.focus();
}

dh.quippopup._end = function() {
	var quipPopup = document.getElementById("dhQuipPopup");
	quipPopup.style.display = "none";
}

dh.quippopup.cancel = function() {
	dh.quippopup._end();
}

dh.quippopup.init = function() {
	dh.chatinput.init();

	dh.chatinput.onCancel = dh.quippopup.cancel;
}
