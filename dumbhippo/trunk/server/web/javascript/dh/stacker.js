var dh = {}
dh.stacker = {}
dh.lang = {}

var dhBaseUrl = "http://localinstance.mugshot.org:8080"


///////////////////////// metalinguistic band-aids

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

dh.stacker.Kind = {};
dh.stacker.Kind.POST = 1;
dh.stacker.Kind.MUSIC = 2;
dh.stacker.Kind.GROUP = 3;
dh.stacker.Kind.NETWORK = 4;

dh.stacker.Block = function(kind) {
	this._kind = kind;
	// the stackTime is the sort key for stacker blocks and is the milliseconds
	// numeric representation of Date()
	this._stackTime = 0;
	
	this._title = null;
	
	// the html div
	this._div = null;
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
	
	realize : function() {
		if (!this._div) {
			this._div = document.createElement("div");
			this._div.style.display = 'none';
			
			dojo.html.setClass(this._div, "dh-stacked-block");

			this._titleDiv = document.createElement("div");
			this._div.appendChild(this._titleDiv);
			dojo.html.setClass(this._titleDiv, "dh-title");
			this._updateTitleDiv();
			
			this._contentDiv = document.createElement("div");
			this._div.appendChild(this._contentDiv);
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
			this._div.parentNode.removeChild(this._div);
			this._div = null;
			// null these just to aid in gc
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
	}
});

dh.stacker.PostBlock = function(postId, title) {
	dh.stacker.Block.call(this, dh.stacker.Kind.POST);
	this._postId = postId;
	this._viewerCount = 0;
	
	this._viewsDiv = null;
	
	this.setTitle(title);
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
	}

});

dh.stacker.MusicBlock = function(userId) {
	dh.stacker.Block.call(this, dh.stacker.Kind.MUSIC);
	this._userId = userId;
}

defineClass(dh.stacker.MusicBlock, dh.stacker.Block,
{
	getUserId : function() {
		return this._userId;
	}

});

dh.stacker.Stacker = function() {
	this._container = null;
	// end of list is top of the screen, highest stackTime
	this._stack = [];
	this._postBlocksById = {};
}

defineClass(dh.stacker.Stacker, null, 
{
	setContainer : function(container) {
		this._container = container;
	},

	onPostChanged : function(postId, title, stackTime, viewerCount) {
		var block = this._postBlocksById[postId];
		if (!block) {
			block = new dh.stacker.PostBlock(postId, title);
			this._postBlocksById[postId] = block;
		}
		if (stackTime == block.getStackTime() &&
			viewerCount == block.getViewerCount())
			return;
			
		block.setStackTime(stackTime);
		block.setViewerCount(viewerCount);
		
		this._updateBlock(block);
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
	// but should not change visibility or anything
	_updateBlockDivLocation : function(block) {
		if (!block._div)
			throw new Error("block is not realized, can't update location");
		
		if (block._div.parentNode)
			this._container.removeChild(block._div);
	
		var olderBlock = this._findOlderBlock(block);
		if (olderBlock)
			this._container.insertBefore(block._div, olderBlock._div);
		else
			this._container.insertBefore(block._div, this._container.firstChild);	
	},
	
	_updateBlock : function(block) {
		var i = this._findBlockInStack(block);
		if (i < 0) {
			var j = this._findInsertPosition(block.getStackTime());
			this._stack.splice(j, 0, block);
			block.realize();
			
			this._updateBlockDivLocation(block);
			
			block.showWithFade();
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
	stacker.onPostChanged(dh.stacker.getFakeGuid(), title, new Date().getTime(), 1);
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

dh.stacker.simulateMoreViews = function(stacker) {
	var block = dh.stacker.getRandomBlock(stacker);
	stacker.onPostChanged(block.getPostId(), block.getTitle(), block.getStackTime(), 
							block.getViewerCount() + 1);
}

dh.stacker.simulateNewStackTime = function(stacker) {
	var block = dh.stacker.getRandomBlock(stacker);
	stacker.onPostChanged(block.getPostId(), block.getTitle(), new Date().getTime(), 
							block.getViewerCount());
}
