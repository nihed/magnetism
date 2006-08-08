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

dh.stacker.kindFromString = function(str) {
	if (str == "POST")
		return dh.stacker.Kind.POST;
	else if (str == "MUSIC_PERSON")
		return dh.stacker.Kind.MUSIC_PERSON;
	else if (str == "GROUP_CHAT")
		return dh.stacker.Kind.GROUP_CHAT;
	else
		return dh.stacker.Kind.UNKNOWN;
}

dh.stacker.Block = function(kind, blockId) {
	this._kind = kind;
	this._blockId = blockId;
	
	// the stackTime is the sort key for stacker blocks and is the milliseconds
	// numeric representation of Date()
	this._stackTime = 0;
	
	this._title = "";
	
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
	
	_updateTitleDiv : function() {
		if (this._div) {
			dojo.dom.textContent(this._titleDiv, this._title);
		}
	},

	_updateStackTimeDiv : function() {
		if (this._div) {
			var d = new Date(this._stackTime);
			dojo.dom.textContent(this._stackTimeDiv, d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds() + ":" + d.getMilliseconds());
		}
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
			dojo.html.setClass(this._innerDiv, "dh-stacked-block");
			this.setNewOuterDiv(this.createOuterDiv());
			this.reparentIntoOuterDiv(this._div);

			this._titleDiv = document.createElement("div");
			this._innerDiv.appendChild(this._titleDiv);
			dojo.html.setClass(this._titleDiv, "dh-title");
			this._updateTitleDiv();
			
			this._contentDiv = document.createElement("div");
			this._innerDiv.appendChild(this._contentDiv);
			dojo.html.setClass(this._contentDiv, "dh-content");
			
			this._stackTimeDiv = document.createElement("div");
			this._contentDiv.appendChild(this._stackTimeDiv);
			dojo.html.setClass(this._stackTimeDiv, "dh-timestamp");
			this._updateStackTimeDiv();
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
			this._titleDiv = null;
			this._contentDiv = null;
			this._stackTimeDiv = null;
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
	
		if (newBlock.getStackTime() <= this.getStackTime()) {
			// new block isn't really newer
			return false;
		}
		
		this.setStackTime(newBlock.getStackTime());
		
		this.setTitle(newBlock.getTitle());
		
		return true;
	}
});

dh.stacker.PostBlock = function(blockId, postId) {
	dh.stacker.Block.call(this, dh.stacker.Kind.POST, blockId);
	this._postId = postId;
	this._viewerCount = 0;
	
	this._viewsDiv = null;
	
	this._link = null;
	this._description = null;
}

defineClass(dh.stacker.PostBlock, dh.stacker.Block,
{
	getPostId : function() {
		return this._postId;
	},
		
	getViewerCount : function() {
		return this._viewerCount;
	},
	
	setViewerCount : function(viewerCount) {
		this._viewerCount = viewerCount;
		this._updateViewsDiv();
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
	
	_updateViewsDiv : function() {
		if (this._div) {
			dojo.dom.textContent(this._viewsDiv, this._viewerCount + " views");
		}
	},
	
	realize : function() {
		if (!this._div) {
			dh.stacker.PostBlock.superclass.realize.call(this);
			this._viewsDiv = document.createElement("div");
			this._contentDiv.appendChild(this._viewsDiv);
			dojo.html.setClass(this._viewsDiv, "dh-views-count");
			this._updateViewsDiv();
		}
	},
	
	unrealize : function() {
		dh.stacker.PostBlock.superclass.unrealize.call(this);	
		this._viewsDiv = null;
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
		this.setViewerCount(newBlock.getViewerCount());
		this.setDescription(newBlock.getDescription());
		this.setLink(newBlock.getLink());
		return true;
	},
	
	_parse : function(childNodes) {
		var post = childNodes.item(0);
		var title = "";
		var text = "";
		var link = "";
		var i;
		for (i = 0; i < post.childNodes.length; ++i) {
			var n = post.childNodes.item(i);
			if (n.nodeName == "title") {
				title = dojo.dom.textContent(n);
			} else if (n.nodeName == "text") {
				text = dojo.dom.textContent(n);
			} else if (n.nodeName == "href") {
				link = dojo.dom.textContent(n);
			}
		}
		this.setTitle(title);
		this.setDescription(text);
		this.setLink(link);
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
		this.setTitle(person.displayName + "'s Music");
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

	_updateTracksDiv : function() {
		if (this._div) {
			var str = "";
			var i;
			for (i = 0; i < this._tracks.length; ++i) {
				str = str + " " + this._tracks[i].title;
			}
			dojo.dom.textContent(this._tracksDiv, str);
		}
	},
	
	realize : function() {
		if (!this._div) {
			dh.stacker.MusicPersonBlock.superclass.realize.call(this);
			this._tracksDiv = document.createElement("div");
			this._contentDiv.appendChild(this._tracksDiv);
			dojo.html.setClass(this._tracksDiv, "dh-tracks");
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

	load : function(completeFunc, errorFunc) {
		this.setTitle("Group chat " + this._groupId);
		completeFunc(this);	
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
		this.setTitle(group.displayName + " Group Chat");
		this.setMessages(messages);
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
			var str = "";
			var i;
			for (i = 0; i < this._messages.length; ++i) {
				str = str + " " + this._messages[i].text;
			}
			dojo.dom.textContent(this._messagesDiv, str);
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
	attrs["clicked"] = dh.stacker.getAttributeBool(node, "clicked");
	attrs["clickedTime"] = dh.stacker.getAttributeInt(node, "clickedTimestamp");
	attrs["ignored"] = dh.stacker.getAttributeBool(node, "ignored");
	attrs["ignoredTime"] = dh.stacker.getAttributeInt(node, "ignoredTimestamp");
	
	return attrs;
}

dh.stacker.mergeBlockAttrs = function(block, attrs) {
	block.setStackTime(attrs["timestamp"]);
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

dh.stacker.Stacker = function() {
	this._container = null;
	// end of list is top of the screen, highest stackTime
	this._stack = [];
	this._blocks = {}; // blocks by block id
	this._poll = null;
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
		}, 5000);
	},

	setContainer : function(container) {
		this._container = container;
	},

	_newBlockLoaded : function(block) {
		if (!block)
			throw new Error("null block in _newBlockLoaded");
			
		var old = this._blocks[block.getBlockId()];
		if (old) {
			// FIXME this breaks if the stack time changes, 
			// updateBlock needs the old time.
			old.updateFrom(block);
		} else {
			this._blocks[block.getBlockId()] = block;
			old = block;
		}
			
		this._updateBlock(old);
	},

	_parseNewBlocks : function(nodes) {
		// get list of children of <blocks>
	    nodes = nodes.item(0).childNodes;
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
					var me = this;
					block.load(function(block) {
						me._newBlockLoaded(block);
					},
					// on failure to load
					function(block) {
					});
				}
			}
		}
	},

	_pollNewBlocks : function() {
		var newestTime;
		if (this._stack.length == 0)
			newestTime = 0;
		else
			newestTime = this._stack[this._stack.length-1].getStackTime();
		
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
	
	_findInsertPosition : function(stackTime) {
		var i;
		for (i = 0; i < this._stack.length; ++i) {
			if (this._stack[i].getStackTime() > stackTime)
				break;
		}
  		// insert before current i; before stack.length for append
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
		
		if (olderBlock)
			this._container.insertBefore(newOuterDiv, olderBlock._div);
		else
			this._container.insertBefore(newOuterDiv, this._container.firstChild);	
		
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
			var anim = new dh.stacker.RaiseAnimation(block, block._innerDiv, oldOuterDiv, newOuterDiv);
			anim.start();
		}
	},
	
	_updateBlock : function(block) {
		if (!block)
			throw new Error("updating null block");
		if (!this._blocks[block.getBlockId()])
			throw new Error("to update block it has to be in _blocks already " + block.getBlockId());
	
		var i = this._findBlockInStack(block);
		if (i < 0) {
			var j = this._findInsertPosition(block.getStackTime());
			this._stack.splice(j, 0, block);
			block.realize();
			
			this._updateBlockDivLocation(block);
		} else {
			// relocate it
			var j = this._findInsertPosition(block.getStackTime());
			if ((i + 1) != j) {
				this._stack.splice(i, 1);
				this._stack.splice(j, 0, block);
				if (block._div) {
					this._updateBlockDivLocation(block);
				}
			}
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
	block.setStackTime(new Date().getTime());
	block.setViewerCount(1);
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
	newBlock.setViewerCount(newViewerCount);
	stacker._newBlockLoaded(newBlock);
}

dh.stacker.simulateMoreViews = function(stacker) {
	var block = dh.stacker.getRandomBlock(stacker);
	if (block.getKind() == dh.stacker.Kind.POST) {
		dh.stacker.simulatePostUpdate(stacker, block, block.getTitle(), block.getStackTime() + 1, 
			block.getViewerCount() + 1);
	}
}

dh.stacker.simulateNewStackTime = function(stacker) {
	var block = dh.stacker.getRandomBlock(stacker);
	if (block.getKind() == dh.stacker.Kind.POST) {
		dh.stacker.simulatePostUpdate(stacker, block, block.getTitle(), new Date().getTime(), 
			block.getViewerCount());
	}
}
