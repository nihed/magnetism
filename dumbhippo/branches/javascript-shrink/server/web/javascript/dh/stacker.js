dojo.provide('dh.stacker');
dojo.require('dh.util');
dojo.require('dh.server');
dojo.require('dh.model');

///////////////////////// metalinguistic band-aids
dh.lang = {};

dh.lang.mixin = function(obj, props){
	var tobj = {};
	for(var x in props){
		if (x == "toString" || typeof tobj[x] == "undefined"){
			obj[x] = props[x];
		}
	}
	return obj;
}

dh.inherits = function(subclass, superclass){
	if(typeof superclass != 'function'){ 
		dh.raise("superclass: "+superclass+" borken");
	}
	subclass.prototype = new superclass();
	subclass.prototype.constructor = subclass;
	subclass.superclass = superclass.prototype;
}

var defineClass = function(childConstructor, parentConstructor, childProps) {
	if (!parentConstructor)
		parentConstructor = Object;
	dh.inherits(childConstructor, parentConstructor);
	dh.lang.mixin(childConstructor.prototype, childProps);
}

///////////////////////// code!

dh.stacker.BLOCK_MARGIN = 6;

dh.stacker.Kind = {};
dh.stacker.Kind.UNKNOWN = -1;
dh.stacker.Kind.POST = 1;
dh.stacker.Kind.MUSIC_PERSON = 2;
dh.stacker.Kind.GROUP_CHAT = 3;
dh.stacker.Kind.GROUP_MEMBER = 4;
dh.stacker.Kind.EXTERNAL_ACCOUNT_UPDATE = 5;

dh.stacker.kindFromString = function(str) {
	if (str == "POST")
		return dh.stacker.Kind.POST;
	else if (str == "MUSIC_PERSON")
		return dh.stacker.Kind.MUSIC_PERSON;
	else if (str == "GROUP_CHAT")
		return dh.stacker.Kind.GROUP_CHAT;
	else if (str == "GROUP_MEMBER")
		return dh.stacker.Kind.GROUP_MEMBER;
	else if (str == "EXTERNAL_ACCOUNT_UPDATE" || str == "EXTERNAL_ACCOUNT_UPDATE_SELF")
	    return dh.stacker.Kind.EXTERNAL_ACCOUNT_UPDATE;	
	else
		return dh.stacker.Kind.UNKNOWN;
}

dh.stacker.kindClasses = {};
dh.stacker.kindClasses[dh.stacker.Kind.POST] = "dh-stacked-block-post";
dh.stacker.kindClasses[dh.stacker.Kind.MUSIC_PERSON] = "dh-stacked-block-music-person";
dh.stacker.kindClasses[dh.stacker.Kind.GROUP_CHAT] = "dh-stacked-block-group-chat";
dh.stacker.kindClasses[dh.stacker.Kind.GROUP_MEMBER] = "dh-stacked-block-group-member";
dh.stacker.kindClasses[dh.stacker.Kind.EXTERNAL_ACCOUNT_UPDATE] = "dh-stacked-block-account-update";

dh.stacker.kindHeadings = {};
dh.stacker.kindHeadings[dh.stacker.Kind.POST] = "Web Swarm";
dh.stacker.kindHeadings[dh.stacker.Kind.MUSIC_PERSON] = "Music Radar";
dh.stacker.kindHeadings[dh.stacker.Kind.GROUP_CHAT] = "Group Chat";
dh.stacker.kindHeadings[dh.stacker.Kind.GROUP_MEMBER] = "Group Members";
dh.stacker.kindHeadings[dh.stacker.Kind.EXTERNAL_ACCOUNT_UPDATE] = "Friend Update";

dh.stacker.formatTimeAgo = function(timestamp) {
	if (timestamp <= 0)
		return "";

	var now = dh.stacker.getInstance().getServerTime();
	var then = timestamp;
	
	var deltaSeconds = (now - timestamp) / 1000;
	
	if (deltaSeconds < 0)
		return "the future";
	
	if (deltaSeconds < 120)
		return "a minute ago";
		
	if (deltaSeconds < 60*60) {
		var deltaMinutes = deltaSeconds / 60;
		if (deltaMinutes < 5) {
			return Math.round(deltaMinutes) + " min. ago";
		} else {
			deltaMinutes = deltaMinutes - (deltaMinutes % 5);
			return Math.round(deltaMinutes) + " min. ago";
		}
	}

	var deltaHours = deltaSeconds / (60 * 60);
	
	if (deltaHours < 1.55) {
		return "1 hr. ago";
	} 

	if (deltaHours < 24) {
		return Math.round(deltaHours) + " hrs. ago";
	}

	if (deltaHours < 48) {
		return "Yesterday";
	}
	
	if (deltaHours < 24*15) {
		return Math.round(deltaHours / 24) + " days ago";
	}
	
	var deltaWeeks = deltaHours / (24*7);
	
	if (deltaWeeks < 6) {
		return Math.round(deltaWeeks) + " weeks ago";
	}
	
	if (deltaWeeks < 50) {
		return Math.round(deltaWeeks / 4) + " months ago";
	}
	
	var deltaYears = deltaWeeks / 52;
	
	if (deltaYears < 1.55) {
		return "1 year ago";
	} else {
		return  Math.round(deltaYears) + " years ago";
	}
}

dh.stacker.Block = function(kind, blockId) {
	this._kind = kind;
	this._blockId = blockId;
	
	// the stackTime is the sort key for stacker blocks and is the milliseconds
	// numeric representation of Date()
	this._stackTime = 0;
	
	// if (_ignored) then _ignoredTime overrides our time
	this._ignored = false;
	this._ignoredTime = 0;
	
	this._title = "";
	this._heading = null;
	
	this._clickedCount = 0;
	
	// the html div
	this._div = null;
	this._innerDiv = null;
	this._titleDiv = null;
	this._contentDiv = null;
	this._stackTimeDiv = null;
	
	// fading
	this._fadeTimer = null;
	this._fadeOpacity = 1.0;
}

defineClass(dh.stacker.Block, null, 
{
	getKind : function() {
		return this._kind;
	},
	
	getBlockId : function() {
		return this._blockId;
	},
	
	getStackTime : function() {
		return this._stackTime;
	},
	
	setStackTime : function(stackTime) {
		this._stackTime = stackTime;
		this._updateStackTimeDiv();
	},
	
	getTitle : function() {
		return this._title;
	},
	
	setTitle : function(title) {
		this._title = title;	
		this._updateTitleDiv();
	},

	getHeading : function() {
		if (this._heading == null)
		    return dh.stacker.kindHeadings[this._kind];
		
		return this._heading;    
	},
	
	setHeading : function(heading) {
		this._heading = heading;	
		this._updateHeadingDiv();
	},
			
	getClickedCount : function() {
		return this._clickedCount;
	},
	
	setClickedCount : function(clickedCount) {
		this._clickedCount = clickedCount;
		this._updateClickedCountDiv();
	},
	
	getIgnored : function() {
		return this._ignored;
	},
	
	setIgnored : function(ignored) {
		if (ignored != this._ignored) {
			this._ignored = ignored;
			this._updateHushDiv();
		}
	},
	
	getIgnoredTime : function() {
		return this._ignoredTime;
	},
	
	setIgnoredTime : function(t) {
		this._ignoredTime = t;
	},
	
	getSortTime : function() {
		if (this._ignored)
			return this._ignoredTime;
		else
			return this._stackTime;
	},
	
	_updateClickedCountDiv : function() {
		// no-op since only some subclasses have a clicked count div
	},

	_updateTitleDiv : function() {
		if (this._div) {
			dojo.dom.textContent(this._titleDiv, this._title);
		}
	},
	
    _updateHeadingDiv : function(headingText) {
		if (this._div) {
			dojo.dom.textContent(this._headingDiv, headingText);
		}	
	},

	_updateStackTimeDiv : function() {
		if (this._div) {
			//var d = new Date(this._stackTime);
			//dojo.dom.textContent(this._stackTimeDiv, d.getFullYear() + "-" + (d.getMonth()+1) + "-" + d.getDate() + " " + d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds() + ":" + d.getMilliseconds());
		}
	},

	_updateHushDiv : function() {
		if (this._div) {
			if (this._ignored)
				dojo.dom.textContent(this._hushDiv, "UNHUSH");
			else
				dojo.dom.textContent(this._hushDiv, "HUSH");
		}
	},

	// called when the server time changes so we need to update the "ago" text
	timeTick : function() {
		this._updateStackTimeDiv();
	},

	createOuterDiv : function() {
		var d = document.createElement("div");
		d.style.display = 'none';
			
		dojo.html.setClass(d, "dh-stacked-block-outer");

		var margin = document.createElement("div");
		margin.style.height = dh.stacker.BLOCK_MARGIN + "px";
		margin.style.width = "10px"; // 0-width can confuse browsers 
		margin.style.overflow = 'hidden'; // otherwise the font size sets a min height
		margin.style.position = 'relative';
		d.appendChild(margin);
		
		return d;
	},
	
	reparentIntoOuterDiv : function(newOuterDiv) {
		if (this._innerDiv.parentNode && this._innerDiv.parentNode != newOuterDiv)
			this._innerDiv.parentNode.removeChild(this._innerDiv);

		// in IE this has the bizarre side effect of setting
		// this._div.parentNode to a document fragment
		if (this._innerDiv.parentNode != this._div)
			this._div.insertBefore(this._innerDiv, this._div.firstChild);
		
		this._cancelFade();	
	},
	
	setNewOuterDiv : function(newOuterDiv) {
		if (this._div == newOuterDiv)
			return;
		this._div = newOuterDiv;
	},
	
	realize : function() {
		if (!this._div) {
			// we have an inner and outer div which helps with the 
			// "div moving" animation
			
			this._innerDiv = document.createElement("div");
			dojo.html.setClass(this._innerDiv, "dh-stacked-block " + dh.stacker.kindClasses[this._kind]);
			this.setNewOuterDiv(this.createOuterDiv());
			this.reparentIntoOuterDiv(this._div);
			
			this._headingDiv = document.createElement("div");
			dojo.html.setClass(this._headingDiv, "dh-heading");
			this._innerDiv.appendChild(this._headingDiv);
			dojo.dom.textContent(this._headingDiv, this.getHeading());
			
			this._hushDiv = document.createElement("div");
			dojo.html.setClass(this._hushDiv, "dh-hush");
			this._headingDiv.appendChild(this._hushDiv);
			this._updateHushDiv();
			
			this._busyImg = document.createElement("img");
			this._busyImg.style.display = "none";
			this._busyImg.src = dhImageRoot2 + "feedspinner.gif";
			this._hushDiv.appendChild(this._busyImg);
	
			var me = this;		
			dh.util.addEventListener(this._hushDiv, "mousedown", function() {
				me._toggleHushed();
			});

			var contentPaddingDiv = document.createElement("div");
			dojo.html.setClass(contentPaddingDiv, "dh-content-padding");		
			this._innerDiv.appendChild(contentPaddingDiv);
			
			this._contentDiv = document.createElement("div");
			dojo.html.setClass(this._contentDiv, "dh-content");			
			contentPaddingDiv.appendChild(this._contentDiv);

			this._titleDiv = document.createElement("div");
			dojo.html.setClass(this._titleDiv, "dh-title");			
			this._contentDiv.appendChild(this._titleDiv);
			this._updateTitleDiv();
			
			//this._stackTimeDiv = document.createElement("div");
			//this._contentDiv.appendChild(this._stackTimeDiv);
			//dojo.html.setClass(this._stackTimeDiv, "dh-timestamp");
			//this._updateStackTimeDiv();
		}
	},
	
	unrealize : function() {
		if (this._div) {
			this._cancelFade();
			if (this._div.parentNode) {
				this._div.parentNode.removeChild(this._div);
			}
			this._div = null;
			// null these just to aid in gc
			this._innerDiv = null;
			this._headingDiv = null;
			this._titleDiv = null;
			this._contentDiv = null;
			this._stackTimeDiv = null;
			this._hushDiv = null;
			this._busyImg = null;
		}
	},

	_setOpacity : function(opacity) {
		this._fadeOpacity = opacity;
		if (this._div)
			dojo.html.setOpacity(this._div, this._fadeOpacity);
	},

	_cancelFade : function() {
		if (this._fadeTimer) {
			clearInterval(this._fadeTimer);
			this._fadeTimer = null;
			// important to do this only if fadeTimer already exists 
			// otherwise we break showWithFade()
			this._setOpacity(1.0);
		}
	},
	
	show : function() {
		if (!this._div)
			throw new Error("must be realized to show block");
		this._cancelFade();
		this._div.style.display = 'block';
	},
	
	showWithFade : function() {
		if (!this._div)
			throw new Error("must be realized to show block");
		if (this._fadeTimer)
			return;
		this._setOpacity(0.0);
		this.show();
		var block = this; // capture for closure
		this._fadeTimer = setInterval(function() {
			var old = block._fadeOpacity;
			if (old > 0.93) {
				block._setOpacity(1.0);
				clearInterval(block._fadeTimer);
				block._fadeTimer = null;
			} else {
				block._setOpacity(old + 0.05);
			}
		}, 50);
	},
	
	load : function(completeFunc, errorFunc) {
		throw new Error("load() not implemented");
	},
	
	// return true if update is needed, used in subclass
	// overrides that chain up to this
	updateFrom : function(newBlock) {
		if (newBlock.getBlockId() != this.getBlockId()) {
			throw new Error("updating block from wrong block");
		}
	
		if (newBlock.getStackTime() <= this.getStackTime() &&
			newBlock.getIgnored() == this.getIgnored() &&
			newBlock.getIgnoredTime() == this.getIgnoredTime()) {
			// new block isn't really changed
			return false;
		}
		
		// don't do this - it messes up our sorted stack.
		// instead it gets done by the stack maintenance code
		// in the appropriate place
		//this.setStackTime(newBlock.getStackTime());
		//this.setIgnoredTime();
		//this.setIgnored();
		
		this.setTitle(newBlock.getTitle());
		this.setClickedCount(newBlock.getClickedCount());
		
		return true;
	},
	
	_toggleHushed : function() {
		this._busyImg.style.display = "inline";
		var me = this;
	   	dh.server.doXmlMethod("setBlockHushed",
				     	{ "blockId" : me._blockId,
				     	  "hushed" : ! me._ignored },
						function(childNodes, http) {
							dh.stacker.getInstance()._parseNewBlocks(childNodes);
							if (me._busyImg)
								me._busyImg.style.display = "none";
			 	    	},
			  	    	function(code, msg, http) {
			  	    		if (me._busyImg)
					  	    	me._busyImg.style.display = "none";
			  	    		alert("Could not hush or unhush: " + msg);
			  	    	});
	}
});

dh.stacker.PostBlock = function(blockId, postId) {
	dh.stacker.Block.call(this, dh.stacker.Kind.POST, blockId);
	this._postId = postId;
	
	this._clickedCountDiv = null;
	this._descriptionDiv = null;
	this._fromDiv = null;
	this._timeDiv = null;
	
	this._link = null;
	this._description = null;
	this._poster = null;
	this._messages = [];
}

defineClass(dh.stacker.PostBlock, dh.stacker.Block,
{
	getPostId : function() {
		return this._postId;
	},
	
	getDescription : function() {
		return this._description;
	}, 
	
	setDescription : function(desc) {
		this._description = desc;
	},
	
	getLink : function() {
		return this._link;
	},
	
	setLink : function(link) {
		this._link = link;
	},
	
	getPoster : function() {
		return this._poster;
	},
	
	setPoster : function(poster) {
		this._poster = poster;
		this._updateFromDiv();
	},

	getMessages : function() {
		return this._messages;
	},

	setMessages : function(messages) {
		this._messages = messages;
		this._updateMessagesDiv();
	},

	// override
	_updateTitleDiv : function() {
		if (this._div) {
			var a = document.createElement('a');
			a.href = "/visit?post=" + this.getPostId();
			a.title = this.getTitle();
			a.target="_blank";
			dojo.dom.textContent(a, this.getTitle());
			if (this._titleDiv.firstChild)
				this._titleDiv.removeChild(this._titleDiv.firstChild);
			this._titleDiv.appendChild(a);
		}
	},

	// override	
	_updateClickedCountDiv : function() {
		if (this._div) {
			dojo.dom.textContent(this._clickedCountDiv, this.getClickedCount() + " views");
		}
	},
	
	_updateDescriptionDiv : function() {
		if (this._div) {
			dojo.dom.textContent(this._descriptionDiv, this.getDescription());
		}
	},
	
	// override
	_updateStackTimeDiv : function() {
		if (this._div) {
			dojo.dom.textContent(this._timeDiv, dh.stacker.formatTimeAgo(this._stackTime));
		}
	},
	
	_updateFromDiv : function() {
		if (this._div) {
			while (this._fromDiv.firstChild)
				this._fromDiv.removeChild(this._fromDiv.firstChild);		
			if (this._poster.homeUrl) {
				var a = document.createElement('a');
				a.href = this._poster.homeUrl;
				a.title = this._poster.displayName;
				a.target = "_blank";
				dojo.dom.textContent(a, this._poster.displayName);
				var from = document.createElement('span');
				dojo.dom.textContent(from, "from ");
				this._fromDiv.appendChild(from);
				this._fromDiv.appendChild(a);
			} else {
				dojo.dom.textContent(this._fromDiv, "from " + this._poster.displayName);
			}
		}
	},

	_updateMessagesDiv : function() {
		if (this._div) {
			while (this._messagesDiv.firstChild)
				this._messagesDiv.removeChild(this._messagesDiv.firstChild);

			var msgs = document.createElement("div");
			dojo.html.setClass(msgs, "dh-group-chat-messages");
			var i;
			for (i = 0; i < this._messages.length; ++i) {
				var msg = document.createElement("div");
				dojo.html.setClass(msg, "dh-post-chat-message");
				dojo.dom.textContent(msg, this._messages[i].text + " - ");
				var a = document.createElement("a");
				dojo.html.setClass(a, "dh-post-chat-message-author");
				a.href = "/person?who=" + this._messages[i].fromId;
				a.title = this._messages[i].fromNickname;
				a.target = "_blank";
				dojo.dom.textContent(a, this._messages[i].fromNickname);
				msg.appendChild(a);
				msgs.appendChild(msg);

			}
			this._messagesDiv.appendChild(msgs);
		}
	},
	
	realize : function() {
		if (!this._div) {
			dh.stacker.PostBlock.superclass.realize.call(this);

			var leftDiv = document.createElement("div");
			dojo.html.setClass(leftDiv, "dh-left-column");
			this._contentDiv.appendChild(leftDiv);

			var rightDiv = document.createElement("div");
			dojo.html.setClass(rightDiv, "dh-right-column");
			this._contentDiv.appendChild(rightDiv);
			
			// move the title created in superclass method
			this._titleDiv.parentNode.removeChild(this._titleDiv);
			leftDiv.appendChild(this._titleDiv);

			this._fromDiv = document.createElement("div");
			rightDiv.appendChild(this._fromDiv);
			dojo.html.setClass(this._fromDiv, "dh-from");
			this._updateFromDiv();

			var detailDiv = document.createElement("div");
			dojo.html.setClass(detailDiv, "dh-details");
			rightDiv.appendChild(detailDiv);	

			this._clickedCountDiv = document.createElement("div");
			detailDiv.appendChild(this._clickedCountDiv);
			dojo.html.setClass(this._clickedCountDiv, "dh-clicked-count");
			this._updateClickedCountDiv();

			var pipeDiv = document.createElement("div");
			dojo.html.setClass(pipeDiv, "dh-pipe");
			dojo.dom.textContent(pipeDiv, "|");
			detailDiv.appendChild(pipeDiv);

			this._timeDiv = document.createElement("div");
			detailDiv.appendChild(this._timeDiv);
			dojo.html.setClass(this._timeDiv, "dh-when");
			this._updateStackTimeDiv();
			
			this._descriptionDiv = document.createElement('div');
			leftDiv.appendChild(this._descriptionDiv);
			dojo.html.setClass(this._descriptionDiv, "dh-description");
			this._updateDescriptionDiv();
			
			this._messagesDiv = document.createElement("div");
			leftDiv.appendChild(this._messagesDiv);
			dojo.html.setClass(this._messagesDiv, "dh-messages");
			this._updateMessagesDiv();
		}
	},
	
	unrealize : function() {
		dh.stacker.PostBlock.superclass.unrealize.call(this);	
		this._clickedCountDiv = null;
		this._descriptionDiv = null;
		this._timeDiv = null;
		this._fromDiv = null;
		this._messagesDiv = null;
	},
	
	load : function(completeFunc, errorFunc) {
		var me = this;
	   	dh.server.doXmlMethod("postsummary",
					     	{ "postId" : me._postId },
							function(childNodes, http) {
								me._parse(childNodes);
								completeFunc(me);
				 	    	},
				  	    	function(code, msg, http) {
								errorFunc(me);
				  	    	});
	},
	
	updateFrom : function(newBlock) {
		if (!dh.stacker.PostBlock.superclass.updateFrom.call(this, newBlock))
			return false;
		this.setDescription(newBlock.getDescription());
		this.setLink(newBlock.getLink());
		this.setPoster(newBlock.getPoster());
		
		// this is a little unkosher since it doesn't make a copy
		this.setMessages(newBlock._messages);
		
		return true;
	},
	
	_parse : function(childNodes) {
		var post = childNodes.item(0);
		var title = "";
		var text = "";
		var link = "";
		var posterId = null;
		var i;
		for (i = 0; i < post.childNodes.length; ++i) {
			var n = post.childNodes.item(i);
			if (n.nodeName == "title") {
				title = dojo.dom.textContent(n);
			} else if (n.nodeName == "text") {
				text = dojo.dom.textContent(n);
			} else if (n.nodeName == "href") {
				link = dojo.dom.textContent(n);
			} else if (n.nodeName == "poster") {
				posterId = dojo.dom.textContent(n);
			}
		}
		this.setTitle(title);
		this.setDescription(text);
		this.setLink(link);
		
		var posterNode = childNodes.item(1);
		var poster = dh.model.objectFromXmlNode(posterNode);
		if (poster.id != posterId)
			throw new Error("mismatched poster ids");
		this.setPoster(poster);
		
		var messages = [];
		for (i = 2; i < childNodes.length; ++i) {
			var messageNode = childNodes.item(i);
			if (messageNode.nodeName != "message")
				throw new Error("message node expected");
			var message = dh.model.messageFromXmlNode(messageNode);
			messages.push(message);
		}
		this.setMessages(messages);
	}
});

dh.stacker.MusicPersonBlock = function(blockId, userId) {
	dh.stacker.Block.call(this, dh.stacker.Kind.MUSIC_PERSON, blockId);
	this._userId = userId;
	this._tracks = [];
	this._tracksDiv = null;
}

defineClass(dh.stacker.MusicPersonBlock, dh.stacker.Block,
{
	getUserId : function() {
		return this._userId;
	},

	getTracks : function() {
		return this._tracks;
	},

	setTracks : function(tracks) {
		this._tracks = tracks;
		this._updateTracksDiv();
	},

	_parse : function(childNodes) {
		var musicPerson = childNodes.item(0);
		if (musicPerson.nodeName != "musicPerson") {
			throw new Error("musicPerson node expected");
		}
		childNodes = musicPerson.childNodes;
		var personNode = childNodes.item(0);
		if (personNode.nodeName != "person")
			throw new Error("person node expected");
		var person = dh.model.personFromXmlNode(personNode);
		var tracks = [];
		var i;
		for (i = 1; i < childNodes.length; ++i) {
			var trackNode = childNodes.item(i);
			if (trackNode.nodeName != "song")
				throw new Error("track node expected");
			var track = dh.model.trackFromXmlNode(trackNode);
			tracks.push(track);
		}
		this.setTitle(person.displayName);
		this.setTracks(tracks);
	},

	load : function(completeFunc, errorFunc) {
		var me = this;
	   	dh.server.doXmlMethod("musicpersonsummary",
					     	{ "userId" : me._userId },
							function(childNodes, http) {
								me._parse(childNodes);
								completeFunc(me);
				 	    	},
				  	    	function(code, msg, http) {
								errorFunc(me);
				  	    	});
	},
	
	updateFrom : function(newBlock) {
		if (!dh.stacker.MusicPersonBlock.superclass.updateFrom.call(this, newBlock))
			return false;
		// this is a little unkosher since it doesn't make a copy
		this.setTracks(newBlock._tracks);
		return true;
	},
	
	// override
	_updateTitleDiv : function() {
		if (this._div) {
			while (this._titleDiv.firstChild)
				this._titleDiv.removeChild(this._titleDiv.firstChild);

			var a = document.createElement("a");
			dojo.html.setClass(a, "dh-person");
			a.href = "/person?who=" + this._userId;
			a.title = this._title;
			a.target = "_blank";
			dojo.dom.textContent(a, this._title);
			this._titleDiv.appendChild(a);
			this._titleDiv.appendChild(document.createTextNode("'s Music"));
		}
	},

	_updateTracksDiv : function() {
		if (this._div) {
			while (this._tracksDiv.firstChild)
				this._tracksDiv.removeChild(this._tracksDiv.firstChild);

			var ul = document.createElement("ul");
			dojo.html.setClass(ul, "dh-song-list");
			var i;
			for (i = 0; i < this._tracks.length; ++i) {
				var li = document.createElement("li");
				dojo.html.setClass(li, "dh-song");
				
				if (this._tracks[i].artist) {
					var artist = document.createElement("a");
					artist.href = "/artist" + dh.util.encodeQueryString( {"artist" : this._tracks[i].artist} )
					artist.target = "_blank";
					dojo.html.setClass(artist, "dh-song-artist");
					dojo.dom.textContent(artist, this._tracks[i].artist);
					li.appendChild(artist);

					var dash = document.createElement("span");
					dojo.dom.textContent(dash, " - ");
					li.appendChild(dash);
				}

				var title = document.createElement("a")
				title.href = "/artist" + dh.util.encodeQueryString( {"artist" : this._tracks[i].artist, "track": this._tracks[i].title} );
				title.target = "_blank";
				dojo.html.setClass(title, "dh-song-title");
				dojo.dom.textContent(title, this._tracks[i].title);
				li.appendChild(title);

				ul.appendChild(li);
			}
			this._tracksDiv.appendChild(ul);
		}
	},
	
	realize : function() {
		if (!this._div) {
			dh.stacker.MusicPersonBlock.superclass.realize.call(this);
			this._tracksDiv = document.createElement("div");
			this._contentDiv.appendChild(this._tracksDiv);
			dojo.html.setClass(this._tracksDiv, "dh-songs");
			this._updateTracksDiv();
		}
	},
	
	unrealize : function() {
		dh.stacker.MusicPersonBlock.superclass.unrealize.call(this);	
		this._tracksDiv = null;
	}
});

dh.stacker.GroupChatBlock = function(blockId, groupId) {
	dh.stacker.Block.call(this, dh.stacker.Kind.GROUP_CHAT, blockId);
	this._groupId = groupId;
	this._messages = [];
}

defineClass(dh.stacker.GroupChatBlock, dh.stacker.Block,
{
	getGroupId : function() {
		return this._groupId;
	},

	getMessages : function() {
		return this._messages;
	},

	setMessages : function(messages) {
		this._messages = messages;
		this._updateMessagesDiv();
	},

	_parse : function(childNodes) {
		var groupChat = childNodes.item(0);
		if (groupChat.nodeName != "groupChat") {
			throw new Error("groupChat node expected");
		}
		childNodes = groupChat.childNodes;
		var groupNode = childNodes.item(0);
		if (groupNode.nodeName != "group")
			throw new Error("group node expected");
		var group = dh.model.groupFromXmlNode(groupNode);
		var messages = [];
		var i;
		for (i = 1; i < childNodes.length; ++i) {
			var messageNode = childNodes.item(i);
			if (messageNode.nodeName != "message")
				throw new Error("message node expected");
			var message = dh.model.messageFromXmlNode(messageNode);
			messages.push(message);
		}
		this.setMessages(messages);
		this.setTitle(group.displayName + " Group Chat");
	},

	load : function(completeFunc, errorFunc) {
		var me = this;
	   	dh.server.doXmlMethod("groupchatsummary",
					     	{ 	"groupId" : me._groupId },
							function(childNodes, http) {
								me._parse(childNodes);
								completeFunc(me);
				 	    	},
				  	    	function(code, msg, http) {
								errorFunc(me);
				  	    	});
	},
	
	updateFrom : function(newBlock) {
		if (!dh.stacker.GroupChatBlock.superclass.updateFrom.call(this, newBlock))
			return false;
		// this is a little unkosher since it doesn't make a copy
		this.setMessages(newBlock._messages);
		return true;
	},

	_updateMessagesDiv : function() {
		if (this._div) {
			while (this._messagesDiv.firstChild)
				this._messagesDiv.removeChild(this._messagesDiv.firstChild);

			var msgs = document.createElement("div");
			dojo.html.setClass(msgs, "dh-group-chat-messages");
			var i;
			for (i = 0; i < this._messages.length; ++i) {
				var msg = document.createElement("div");
				dojo.html.setClass(msg, "dh-group-chat-message");
				dojo.dom.textContent(msg, this._messages[i].text + " - ");
				var a = document.createElement("a");
				dojo.html.setClass(a, "dh-group-chat-message-author");
				a.href = "/person?who=" + this._messages[i].fromId;
				a.title = this._messages[i].fromNickname;
				a.target = "_blank";
				dojo.dom.textContent(a, this._messages[i].fromNickname);
				msg.appendChild(a);
				msgs.appendChild(msg);
			}
			this._messagesDiv.appendChild(msgs);
		}
	},
	
	realize : function() {
		if (!this._div) {
			dh.stacker.GroupChatBlock.superclass.realize.call(this);
			this._messagesDiv = document.createElement("div");
			this._contentDiv.appendChild(this._messagesDiv);
			dojo.html.setClass(this._messagesDiv, "dh-messages");
			this._updateMessagesDiv();
		}
	},
	
	unrealize : function() {
		dh.stacker.GroupChatBlock.superclass.unrealize.call(this);	
		this._messagesDiv = null;
	}	
});

dh.stacker.GroupMemberBlock = function(blockId, groupId, userId) {
	dh.stacker.Block.call(this, dh.stacker.Kind.GROUP_MEMBER, blockId);
	this._groupId = groupId;
	this._userId = userId;
}

defineClass(dh.stacker.GroupMemberBlock, dh.stacker.Block,
{
	getGroupId : function() {
		return this._groupId;
	},

	getUserId : function() {
		return this._userId;
	},

	_parse : function(childNodes) {

	},

	load : function(completeFunc, errorFunc) {
		this.setTitle(" has a new group member ");
		completeFunc(this);		
	},
	
	updateFrom : function(newBlock) {
		if (!dh.stacker.GroupMemberBlock.superclass.updateFrom.call(this, newBlock))
			return false;

		return true;
	},
	
	realize : function() {
		if (!this._div) {
			dh.stacker.GroupMemberBlock.superclass.realize.call(this);

		}
	},
	
	unrealize : function() {
		dh.stacker.GroupMemberBlock.superclass.unrealize.call(this);	

	},

	// override
	_updateTitleDiv : function() {
		if (this._div) {
			while (this._titleDiv.firstChild)
				this._titleDiv.removeChild(this._titleDiv.firstChild);

			var group = document.createElement("a");
			group.href = "/group?who=" + this._groupId;
			group.title = "A group title would be nice";
			group.target = "_blank";
			dojo.dom.textContent(group, this._groupId);
			this._titleDiv.appendChild(group);
			
			var hasa = document.createElement("span");
			dojo.dom.textContent(hasa, this.getTitle());
			this._titleDiv.appendChild(hasa);

			var member = document.createElement('a');
			member.href = "/person?who=" + this._userId;
			member.title = "A person name would be nice";
			member.target="_blank";
			dojo.dom.textContent(member, this._userId);
			this._titleDiv.appendChild(member);
		}
	}
});

dh.stacker.AccountUpdateBlock = function(blockId, userId, accountType) {
	dh.stacker.Block.call(this, dh.stacker.Kind.EXTERNAL_ACCOUNT_UPDATE, blockId);
	this._userId = userId;
	this._accountType = accountType;

    this._items = [];		
	this._itemsDiv = null;
	
	this._timeDivs = [];
	// possibly also need this._accountTypeStr
}

defineClass(dh.stacker.AccountUpdateBlock, dh.stacker.Block,
{
	getUserId : function() {
		return this._userId;
	},

    getAccountType : function() {
        return this._accountType;
    },

	getItems : function() {
		return this._items;
	},

	setItems : function(items) {
		this._items = items;
		this._updateItemsDiv();
	},	
		
	_parse : function(childNodes) {
		var accountUpdate = childNodes.item(0);
		if (accountUpdate.nodeName != "accountUpdate") {
			throw new Error("accountUpdate node expected");
		}
		childNodes = accountUpdate.childNodes;
		var personNode = childNodes.item(0);
		if (personNode.nodeName != "person")
			throw new Error("person node expected");
		var person = dh.model.personFromXmlNode(personNode);

		var accountTypeNode = childNodes.item(1);
		if (accountTypeNode.nodeName != "accountType")
			throw new Error("accountType node expected");
		var accountType = dojo.dom.textContent(accountTypeNode);	
		
		var items = [];
		var i;
		for (i = 2; i < childNodes.length; ++i) {
			var itemNode = childNodes.item(i);
			var item = dh.model.updateItemFromXmlNode(itemNode);
			items.push(item);
		}
				
		this.setHeading(person.displayName + "'s " + accountType);
		this.setItems(items);
	},

	load : function(completeFunc, errorFunc) {
		var me = this;
	   	dh.server.doXmlMethod("externalaccountsummary",
					     	{ "userId" : me._userId,
					     	  "accountType" : me._accountType },
							function(childNodes, http) {
								me._parse(childNodes);
								completeFunc(me);
				 	    	},
				  	    	function(code, msg, http) {
								errorFunc(me);
				  	    	});
	},
	
	updateFrom : function(newBlock) {
		if (!dh.stacker.AccountUpdateBlock.superclass.updateFrom.call(this, newBlock))
			return false;	
		this.setHeading(newBlock.getHeading());
		this.setItems(newBlock.getItems());				
		return true;
	},
	
	_updateItemsDiv : function() {
	    if (this._div) {
			while (this._itemsDiv.firstChild)
				this._itemsDiv.removeChild(this._itemsDiv.firstChild);
            this._timeDivs = []; 

			var items = document.createElement("div");
			dojo.html.setClass(items, "dh-account-update-items");
			var i;
			for (i = 0; i < this._items.length; ++i) {
				var item = document.createElement("div");
				dojo.html.setClass(item, "dh-account-update-item");

			    var leftDiv = document.createElement("div");
			    dojo.html.setClass(leftDiv, "dh-left-column");
			    item.appendChild(leftDiv);

			    var rightDiv = document.createElement("div");
			    dojo.html.setClass(rightDiv, "dh-right-column");
			    item.appendChild(rightDiv);
			
			    var titleDiv = document.createElement("div");
			    dojo.html.setClass(titleDiv, "dh-title");	
			    var a = document.createElement('a');
			    a.href = this._items[i].link;
			    a.title = this._items[i].title;
			    a.target="_blank";
			    dojo.dom.textContent(a, this._items[i].title);
			    titleDiv.appendChild(a);
			    leftDiv.appendChild(titleDiv);

			    var detailDiv = document.createElement("div");
			    dojo.html.setClass(detailDiv, "dh-details");
			    rightDiv.appendChild(detailDiv);	
			
		        var timeDiv = document.createElement("div");
			    detailDiv.appendChild(timeDiv);
			    dojo.html.setClass(timeDiv, "dh-when");
			    var time = this._items[i].timestamp;
			    if (time == null)
			        time = this._stackTime;
			    dojo.dom.textContent(timeDiv, dh.stacker.formatTimeAgo(time));
			    this._timeDivs.push(timeDiv);

	            var textDiv = document.createElement("div");
	            leftDiv.appendChild(textDiv);
	            dojo.html.setClass(textDiv, "dh-description");
			    dojo.dom.textContent(textDiv, this._items[i].text);
	
	            if (this._items[i].photos.length > 0) {
	                var photosDiv = document.createElement("div");	   			   
			        dojo.html.setClass(photosDiv, "dh-photos");
			        var j;
			        for (j = 0; j < this._items[i].photos.length; ++j) {
			            var photo =  this._items[i].photos[j];
			            var photoDiv = document.createElement("div");	
			            dojo.html.setClass(photoDiv, "dh-photo");
			            var a = document.createElement('a');
			            a.href = photo.link;
		  	            a.title = photo.caption;
			            a.target="_blank";
			            var photoImg = document.createElement("img");
			            photoImg.src = photo.source;
			            a.appendChild(photoImg);
			            photoDiv.appendChild(a);
			            photosDiv.appendChild(photoDiv);
			        }		        
			        leftDiv.appendChild(photosDiv);
			    }    
			        
			    items.appendChild(item);
			}
			this._itemsDiv.appendChild(items);
		}
	},
	
	// override
	_updateStackTimeDiv : function() {
		if (this._div) {
		    for (i = 0; i < this._items.length; ++i) {	
			    var time = this._items[i].timestamp;
			    if (time == null)
			        time = this._stackTime;		
	            dojo.dom.textContent(this._timeDivs[i], dh.stacker.formatTimeAgo(time));
	        }
	    }
	},
	
	realize : function() {
		if (!this._div) {		
			dh.stacker.AccountUpdateBlock.superclass.realize.call(this);			
			this._itemsDiv = document.createElement("div");
			this._contentDiv.appendChild(this._itemsDiv);
			dojo.html.setClass(this._itemsDiv, "dh-items");
			this._updateItemsDiv();
		}
	},
	
	unrealize : function() {
		dh.stacker.AccountUpdateBlock.superclass.unrealize.call(this);
		this._timeDivs = [];
		this._itemsDiv = null;
	}
});


dh.stacker.RaiseAnimation = function(block, inner, oldOuter, newOuter) {

	if (block._div != newOuter) {
		throw new Error("block._div should be newOuterDiv");
	}
	if (oldOuter == newOuter) {
		throw new Error("can't have same div for old and new");
	}

	this._block = block;
	this._inner = inner;
	this._oldOuter = oldOuter;
	this._newOuter = newOuter;
	this._interval = null;
	this._percentage = 0.0;	
}

defineClass(dh.stacker.RaiseAnimation, null, 
{
	_updateOuterHeights : function() {
		var h = this._inner.offsetHeight + dh.stacker.BLOCK_MARGIN;
		
		this._oldOuter.style.height = (h * (1.0 - this._percentage)) + "px";
		this._newOuter.style.height = (h * this._percentage) + "px";
	},

	_updateInnerPosition : function() {
		var oldPos = dh.util.getBodyPosition(this._oldOuter);
		var newPos = dh.util.getBodyPosition(this._newOuter);
		
		// oldPos.x == newPos.x so pick either
		this._inner.style.left = oldPos.x + "px"; 
		
		this._inner.style.top = (newPos.y + (oldPos.y - newPos.y) * (1.0 - this._percentage)) + "px";
	},

	start : function() {
		if (this._interval)
			return;
		
		this._updateOuterHeights();
		this._newOuter.style.display = 'block';
		
		this._inner.style.position = 'absolute';
		this._inner.parentNode.removeChild(this._inner);
		document.body.appendChild(this._inner);
		this._updateInnerPosition();
		
		var anim = this;
		this._interval = setInterval(function() {
		
			if (false && anim._percentage >= 0.5) {
				clearInterval(this._interval);
				this._interval = null;
				return;
			}
		
			if (anim._percentage >= 0.97) {
				anim.finish();
				return;
			}
			
			// we want to grow the new outer, shrink old outer, 
			// and move the inner
			anim._updateOuterHeights();
			anim._updateInnerPosition();
			
			anim._percentage = anim._percentage + 0.05;

		}, 50);
	},
	
	finish : function() {
		if (!this._interval)
			return;
		clearInterval(this._interval);
		this._interval = null;
		this._inner.parentNode.removeChild(this._inner);
		this._inner.style.top = "0px"
		this._inner.style.left = "0px"		
		this._inner.style.position = 'relative';
		this._newOuter.insertBefore(this._inner, this._newOuter.firstChild);
		this._newOuter.style.height = "auto";
		this._oldOuter.parentNode.removeChild(this._oldOuter);
					
		// we already did the reparent but call this for any 
		// side effects
		this._block.reparentIntoOuterDiv(this._newOuter);
		this._block.show();
		this._block._anim = null;
	}
});

dh.stacker.getAttribute = function(node, name) {
	var v = node.getAttribute(name);
	if (!v)
		throw new Error("node " + node.nodeName + " missing attribute " + name);
	return v;
}

dh.stacker.getAttributeInt = function(node, name) {
	var v = dh.stacker.getAttribute(node, name);
	var i = parseInt(v);
	if (i == NaN)
		throw new Error("failed to parse '" + v + "' as an integer on node " + node.nodeName + " attr " + name);
	return i;
}

dh.stacker.getAttributeBool = function(node, name) {
	return dh.stacker.getAttribute(node, name) == "true";
}

dh.stacker.parseBlockAttrs = function(node) {
	var attrs = {};
	attrs["id"] = dh.stacker.getAttribute(node, "id");
	attrs["timestamp"] = dh.stacker.getAttributeInt(node, "timestamp");
	attrs["clickedCount"] = dh.stacker.getAttributeInt(node, "clickedCount");
	attrs["clicked"] = dh.stacker.getAttributeBool(node, "clicked");
	attrs["clickedTime"] = dh.stacker.getAttributeInt(node, "clickedTimestamp");
	attrs["ignored"] = dh.stacker.getAttributeBool(node, "ignored");
	attrs["ignoredTime"] = dh.stacker.getAttributeInt(node, "ignoredTimestamp");
	
	return attrs;
}

dh.stacker.mergeBlockAttrs = function(block, attrs) {
	block.setStackTime(attrs["timestamp"]);
	block.setClickedCount(attrs["clickedCount"]);
	block.setIgnored(attrs["ignored"]);
	block.setIgnoredTime(attrs["ignoredTime"]);
}

dh.stacker.blockParsers = {};
dh.stacker.blockParsers[dh.stacker.Kind.POST] = function(node) {
	var attrs = dh.stacker.parseBlockAttrs(node);
	var post = node.childNodes.item(0);
	if (post.nodeName != "post")
		return null;
	var postId = post.getAttribute("postId");
	var block = new dh.stacker.PostBlock(attrs["id"], postId);
	dh.stacker.mergeBlockAttrs(block, attrs);
	return block;
};

dh.stacker.blockParsers[dh.stacker.Kind.GROUP_CHAT] = function(node) {
	var attrs = dh.stacker.parseBlockAttrs(node);
	var groupChat = node.childNodes.item(0);
	if (groupChat.nodeName != "groupChat")
		return null;
	var groupId = groupChat.getAttribute("groupId");
	var block = new dh.stacker.GroupChatBlock(attrs["id"], groupId);
	dh.stacker.mergeBlockAttrs(block, attrs);
	return block;
};

dh.stacker.blockParsers[dh.stacker.Kind.GROUP_MEMBER] = function(node) {
	var attrs = dh.stacker.parseBlockAttrs(node);
	var groupMember = node.childNodes.item(0);
	if (groupMember.nodeName != "groupMember")
		return null;
	var userId = groupMember.getAttribute("userId");		
	var groupId = groupMember.getAttribute("groupId");
	var block = new dh.stacker.GroupMemberBlock(attrs["id"], groupId, userId);
	dh.stacker.mergeBlockAttrs(block, attrs);
	return block;
};

dh.stacker.blockParsers[dh.stacker.Kind.MUSIC_PERSON] = function(node) {
	var attrs = dh.stacker.parseBlockAttrs(node);
	var musicPerson = node.childNodes.item(0);
	if (musicPerson.nodeName != "musicPerson")
		return null;
	var userId = musicPerson.getAttribute("userId");
	var block = new dh.stacker.MusicPersonBlock(attrs["id"], userId);
	dh.stacker.mergeBlockAttrs(block, attrs);
	return block;
};

dh.stacker.blockParsers[dh.stacker.Kind.EXTERNAL_ACCOUNT_UPDATE] = function(node) {
	var attrs = dh.stacker.parseBlockAttrs(node);
	var extAccountUpdate = node.childNodes.item(0);
	if (extAccountUpdate.nodeName != "extAccountUpdate")
		return null;
	var userId = extAccountUpdate.getAttribute("userId");
	var accountType = extAccountUpdate.getAttribute("accountType");
	var block = new dh.stacker.AccountUpdateBlock(attrs["id"], userId, accountType);
	dh.stacker.mergeBlockAttrs(block, attrs);
	return block;
};

dh.stacker.Stacker = function() {
	this._container = null;
	// end of list is top of the screen, highest stackTime
	this._stack = [];
	this._blocks = {}; // blocks by block id
	this._poll = null;
	// the server time is the time on the server when our last getBlocks
	// request returned
	this._serverTime = 0;
}

defineClass(dh.stacker.Stacker, null, 
{
	start : function() {
		if (this._poll) {
			throw new Error("stacker started twice");
		}
	
		this._pollNewBlocks();
		
		var me = this;
		this._poll = setInterval(function() {
			me._pollNewBlocks();
		}, 10000);
	},

	getServerTime : function() {
		return this._serverTime;
	},

	setContainer : function(container) {
		this._container = container;
	},

	_newBlockLoaded : function(block) {
		if (!block)
			throw new Error("null block in _newBlockLoaded");
			
		var old = this._blocks[block.getBlockId()];
		if (old) {
			if (!old.updateFrom(block))
				return; // new block not really newer
		} else {
			this._blocks[block.getBlockId()] = block;
			old = block;
		}

		this._updateBlock(old, block.getStackTime(), block.getIgnored(), block.getIgnoredTime());
	},

	_parseNewBlocks : function(nodes) {
		// get list of children of <blocks>
		var blocksNode = nodes.item(0);
		this._serverTime = parseInt(blocksNode.getAttribute("serverTime"));
	    nodes = blocksNode.childNodes;
		var i = 0;
		for (i = 0; i < nodes.length; ++i) {
			var child = nodes.item(i);
			if (child.nodeType != dojo.dom.ELEMENT_NODE)
				continue;
		
			if (child.nodeName == "block") {
				var blockType = child.getAttribute("type");

				var kind = dh.stacker.kindFromString(blockType);
				var parseFunc = dh.stacker.blockParsers[kind];
				if (!parseFunc)
					continue;
				var block = parseFunc(child);
				if (block) {
					this.reloadDetails(block);
				}
			}
		}
		for (i = 0; i < this._stack.length; ++i) {
			this._stack[i].timeTick();
		}
	},

	// loads the details for a block, either when we 
	// think the block has changed, or when we're 
	// loading up a bunch of blocks for the first time
	reloadDetails : function(block) {
		var me = this;
		block.load(function(block) {
			me._newBlockLoaded(block);
		},
		// on failure to load
		function(block) {
		});
	},

	_pollNewBlocks : function() {
		// we need newest stackTime, not newest sortTime, so 
		// can't just look at the last block
		var newestTime = 0;
		var i;
		for (i = 0; i < this._stack.length; ++i) {
			newestTime = Math.max(this._stack[i].getStackTime(), newestTime);
		}
		
		// this only gets the first page of blocks, this means we could miss some stuff if
		// >pageSize blocks have been updated since the last attempt. We ask for 
		// few blocks to prime things since we'd expect to always have an infinite list
		// available, then ask for lots of blocks later to minimize chances of missing 
		// something (though we expect few blocks)
		var pageSize = (newestTime == 0) ? 10 : 50;
		var me = this;
	   	dh.server.doXmlMethod("blocks",
					     	{ 	"lastTimestamp" : newestTime,
					     		"start" : 0,
					     		"count" : pageSize },
							function(childNodes, http) {
								me._parseNewBlocks(childNodes);		
				 	    	},
				  	    	function(code, msg, http) {
				  	    		// failed!
				  	    		//alert("failed to update: " + msg);
				  	    	});
	},
	
	_findBlockInStack : function(block) {
		var i;
		for (i = 0; i < this._stack.length; ++i) {
			if (this._stack[i] == block)
				return i;		
		}
		return -1;
	},
	
	_findInsertPosition : function(sortTime) {
		var i;
		for (i = 0; i < this._stack.length; ++i) {
			if (this._stack[i].getSortTime() > sortTime)
				break;
		}
  		// insert at current i; at stack.length for append
		return i;
	},
	
	_findNewerBlock : function(block) {
		var i = this._findBlockInStack(block);
		if (i < 0)
			throw new Error("can't find block after because argument not in stack");
		if (i == this._stack.length - 1)
			return null;
		else
			return this._stack[i+1];
	},
	
	_findOlderBlock : function(block) {
		var i = this._findBlockInStack(block);
		if (i < 0)
			throw new Error("can't find block before because argument not in stack");
		if (i == 0)
			return null;
		else
			return this._stack[i-1];
	},

	// puts the block's div in the right place in _container,
	// and either animates the move or the initial appearance of 
	// the block
	_updateBlockDivLocation : function(block) {
		if (!block._div)
			throw new Error("block is not realized, can't update location");
		
		if (block._anim)
			block._anim.finish();
		if (block._anim)
			throw new Error("finishing anim on block should have cleared it");
		
		var oldOuterDiv;
		var newOuterDiv;
		if (block._div.parentNode == this._container) {
			oldOuterDiv = block._div;
			newOuterDiv = block.createOuterDiv();
		} else {
			oldOuterDiv = block._div;
			newOuterDiv = block._div;
		}

		if (newOuterDiv.parentNode == this._container)
			throw new Error("newOuterDiv should not have parentNode");
			
		var olderBlock = this._findOlderBlock(block);
		
		if (olderBlock == block)
			throw new Error("found ourselves for olderBlock");

		if (newOuterDiv.parentNode)
			newOuterDiv.parentNode.removeChild(newOuterDiv);				
		if (olderBlock) {
			if (olderBlock.getSortTime() > block.getSortTime())
				throw new Error("olderBlock not older");
			if (!olderBlock._div)
				throw new Error("olderBlock not realized");
			if (olderBlock._div.parentNode != this._container)
				throw new Error("olderBlock._div has wrong parent");
			this._container.insertBefore(newOuterDiv, olderBlock._div);
		} else {
			// we're the oldest (or only) block, so we go on the end
			this._container.appendChild(newOuterDiv);
		}
		if (newOuterDiv.parentNode != this._container)
			throw new Error("newOuterDiv not in the container");
		
		if (oldOuterDiv == newOuterDiv) {
			block.showWithFade();
		} else {
			if (!oldOuterDiv.parentNode)
				throw new Error("oldOuterDiv has no parent for some reason " + oldOuterDiv);
			if (oldOuterDiv.parentNode != this._container)
				throw new Error("old outer div " + oldOuterDiv + " has wrong parent " + oldOuterDiv.parentNode);
			// note this doesn't reparent into the new div yet, but we want to 				
			// have the invariant that block._div is accurate
			block.setNewOuterDiv(newOuterDiv);
			block._anim = new dh.stacker.RaiseAnimation(block, block._innerDiv, oldOuterDiv, newOuterDiv);
			block._anim.start();
		}
		if (block._div != newOuterDiv)
			throw new Error("block._div is wrong");
	},
	
	_updateBlock : function(block, newStackTime, newIgnored, newIgnoredTime) {
		if (!block)
			throw new Error("updating null block");
		if (!this._blocks[block.getBlockId()])
			throw new Error("to update block it has to be in _blocks already " + block.getBlockId());
	
		this._checkInvariants();
	
		var newSortTime = newIgnored ? newIgnoredTime : newStackTime;
	
		var i = this._findBlockInStack(block);
		if (i < 0) {
			var j = this._findInsertPosition(newSortTime);
			this._stack.splice(j, 0, block);
			
			block.setStackTime(newStackTime);
			block.setIgnored(newIgnored);
			block.setIgnoredTime(newIgnoredTime);
			
			block.realize();
			
			this._updateBlockDivLocation(block);
		} else {
			// relocate it
			var j = this._findInsertPosition(newSortTime);
			block.setStackTime(newStackTime);
			block.setIgnored(newIgnored);
			block.setIgnoredTime(newIgnoredTime);
			
			if ((i + 1) != j) {
				var removed = this._stack.splice(i, 1);
				if (removed[0] != block)
					throw new Error("removed the wrong thing: " + removed);
				// insert position may have changed due to deleting ourselves
				j = this._findInsertPosition(newSortTime);
				this._stack.splice(j, 0, block);
				this._checkStackInOrder();
				if (block._div) {
					this._updateBlockDivLocation(block);
				}
			}
		}
		this._checkInvariants();
	},
	
	_checkStackInOrder : function() {
		var last = 0;
		var i;
		for (i = 0; i < this._stack.length; ++i) {
			if (this._stack[i].getSortTime() < last) {
				throw new Error("stack out of order");
			}
			last = this._stack[i].getSortTime();
		}	
	},
	
	_checkInvariants : function() {
		this._checkStackInOrder();
		// _container can have old divs during animations,
		// but all "current" block._div should be in the right 
		// order
		if (this._container.childNodes.length < this._stack.length) {
			throw new Error("some item in the stack has no _div");
		}
		var i, j;
		i = this._stack.length - 1; // newest block (first in the container)
		for (j = 0; j < this._container.childNodes.length; ++j) {
			var node = this._container.childNodes.item(j);
			if (i < 0 || this._stack[i]._div != node)
				continue; // an "old" div no longer tied to a block, or an out-of-order div
			--i;
		}
		if (i != -1) {
			throw new Error("block divs in wrong order, i=" + i + " j=" + j);
		}
	}
});

dh.stacker.theInstance = new dh.stacker.Stacker();
dh.stacker.getInstance = function() {
	return dh.stacker.theInstance;
}

dh.stacker.nextFakeGuid = 0;
dh.stacker.getFakeGuid = function() {
	var guid = "" + dh.stacker.nextFakeGuid;
	dh.stacker.nextFakeGuid = dh.stacker.nextFakeGuid + 1;
	guid = guid + ("1234567890abcd").substring(guid.length);
	if (guid.length != 14)
		throw new Error("generated bad fake guid");
	return guid;
};

dh.stacker.simulateNewPost = function(stacker, title) {
	var block = new dh.stacker.PostBlock(dh.stacker.getFakeGuid(), dh.stacker.getFakeGuid());
	block.setTitle(title);
	block.setStackTime(dh.stacker.getInstance().getServerTime());
	block.setClickedCount(1);
	stacker._newBlockLoaded(block);
}

dh.stacker.getRandomBlock = function(stacker) {
	if (stacker._stack.length == 0)
		return;
	var r = Math.random();
	var i = Math.floor(r * stacker._stack.length);
	if (i >= stacker._stack.length || i < 0)
		throw new Error("generated bad random index " + i + " random " + r + " len " + stacker._stack.length);
	var block = stacker._stack[i];
	return block;
}

dh.stacker.simulatePostUpdate = function(stacker, oldBlock, newTitle, newTime, newViewerCount) {
	var newBlock = new dh.stacker.PostBlock(oldBlock.getBlockId(), oldBlock.getPostId());
	newBlock.setTitle(newTitle);
	newBlock.setStackTime(newTime);
	newBlock.setClickedCount(newViewerCount);
	stacker._newBlockLoaded(newBlock);
}

dh.stacker.simulateMoreViews = function(stacker) {
	var block = dh.stacker.getRandomBlock(stacker);
	if (block.getKind() == dh.stacker.Kind.POST) {
		dh.stacker.simulatePostUpdate(stacker, block, block.getTitle(), block.getStackTime() + 1, 
			block.getClickedCount() + 1);
	}
}

dh.stacker.simulateNewStackTime = function(stacker) {
	var block = dh.stacker.getRandomBlock(stacker);
	if (block.getKind() == dh.stacker.Kind.POST) {
		dh.stacker.simulatePostUpdate(stacker, block, block.getTitle(), dh.stacker.getInstance().getServerTime(), 
			block.getClickedCount());
	}
}

dh.stacker.onBlockClick = function(id) {
	var block = document.getElementById("dhStackerBlock-" + id)
	block.style.cursor = "default"
	var content = document.getElementById("dhStackerBlockContent-" + id)
	content.style.display = "block";
	var controls = document.getElementById("dhStackerBlockControls-" + id)
	if (controls)
		controls.style.display = "block";		
	var closeButton = document.getElementById("dhStackerBlockClose-" + id)
	closeButton.style.display = "block";
}

dh.stacker.blockClose = function(id) {
	var block = document.getElementById("dhStackerBlock-" + id)	
	block.style.cursor = "pointer"	
	var content = document.getElementById("dhStackerBlockContent-" + id)
	content.style.display = "none";	
	var controls = document.getElementById("dhStackerBlockControls-" + id)
	if (controls)
		controls.style.display = "none";		
	var closeButton = document.getElementById("dhStackerBlockClose-" + id)
	closeButton.style.display = "none";	
}
