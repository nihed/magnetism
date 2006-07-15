dojo.provide("dojo.fx.html");

dojo.require("dojo.html");
dojo.require("dojo.style");
dojo.require("dojo.lang");
dojo.require("dojo.animation.*");
dojo.require("dojo.event.*");

dojo.fx.html.fadeOut = function(node, duration, callback) {
	return dojo.fx.html.fade(node, duration, dojo.html.getOpacity(node), 0, callback);
};

dojo.fx.html.fadeIn = function(node, duration, callback) {
	return dojo.fx.html.fade(node, duration, dojo.html.getOpacity(node), 1, callback);
};

dojo.fx.html.fadeHide = function(node, duration, callback) {
	if(!duration) { duration = 150; } // why not have a default?
	return dojo.fx.html.fadeOut(node, duration, function(node) {
		node.style.display = "none";
		if(typeof callback == "function") { callback(node); }
	});
};

dojo.fx.html.fadeShow = function(node, duration, callback) {
	if(!duration) { duration = 150; } // why not have a default?
	node.style.display = "block";
	return dojo.fx.html.fade(node, duration, 0, 1, callback);
};

dojo.fx.html.fade = function(node, duration, startOpac, endOpac, callback) {
	var anim = new dojo.animation.Animation(
		new dojo.math.curves.Line([startOpac],[endOpac]),
		duration, 0);
	dojo.event.connect(anim, "onAnimate", function(e) {
		dojo.html.setOpacity(node, e.x);
	});
	if(callback) {
		dojo.event.connect(anim, "onEnd", function(e) {
			callback(node, anim);
		});
	}
	anim.play(true);
	return anim;
};

dojo.fx.html.slideTo = function(node, endCoords, duration, callback) {
	return dojo.fx.html.slide(node, [node.offsetLeft, node.offsetTop], endCoords,
		duration, callback);
};

dojo.fx.html.slideBy = function(node, coords, duration, callback) {
	return dojo.fx.html.slideTo(node, [node.offsetLeft+coords[0], node.offsetTop+coords[1]],
		duration, callback);
};

dojo.fx.html.slide = function(node, startCoords, endCoords, duration, callback) {
	var anim = new dojo.animation.Animation(
		new dojo.math.curves.Line(startCoords, endCoords),
		duration, 0);
	dojo.event.connect(anim, "onAnimate", function(e) {
		with( node.style ) {
			left = e.x + "px";
			top = e.y + "px";
		}
	});
	if(callback) {
		dojo.event.connect(anim, "onEnd", function(e) {
			callback(node, anim);
		});
	}
	anim.play(true);
	return anim;
};

// Fade from startRGB to the node's background color
dojo.fx.html.colorFadeIn = function(node, startRGB, duration, delay, callback) {
	var color = dojo.html.getBackgroundColor(node);
	var bg = dojo.style.getStyle(node, "background-color").toLowerCase();
	var wasTransparent = bg == "transparent" || bg == "rgba(0, 0, 0, 0)";
	while(color.length > 3) { color.pop(); }
	while(startRGB.length > 3) { startRGB.pop(); }

	var anim = dojo.fx.html.colorFade(node, startRGB, color, duration, callback, true);
	dojo.event.connect(anim, "onEnd", function(e) {
		if( wasTransparent ) {
			node.style.backgroundColor = "transparent";
		}
	});
	if( delay > 0 ) {
		node.style.backgroundColor = "rgb(" + startRGB.join(",") + ")";
		setTimeout(function(){anim.play(true)}, delay);
	} else {
		anim.play(true);
	}
	return anim;
};
// alias for (probably?) common use/terminology
dojo.fx.html.highlight = dojo.fx.html.colorFadeIn;
dojo.fx.html.colorFadeFrom = dojo.fx.html.colorFadeIn;

// Fade from node's background color to endRGB
dojo.fx.html.colorFadeOut = function(node, endRGB, duration, delay, callback) {
	var color = dojo.html.getBackgroundColor(node);
	while(color.length > 3) { color.pop(); }
	while(endRGB.length > 3) { endRGB.pop(); }

	var anim = dojo.fx.html.colorFade(node, color, endRGB, duration, callback, delay > 0);
	if( delay > 0 ) {
		node.style.backgroundColor = "rgb(" + color.join(",") + ")";
		setTimeout(function(){anim.play(true)}, delay);
	}
	return anim;
};
// FIXME: not sure which name is better. an alias here may be bad.
dojo.fx.html.unhighlight = dojo.fx.html.colorFadeOut;
dojo.fx.html.colorFadeTo = dojo.fx.html.colorFadeOut;

// Fade node background from startRGB to endRGB
dojo.fx.html.colorFade = function(node, startRGB, endRGB, duration, callback, dontPlay) {
	while(startRGB.length > 3) { startRGB.pop(); }
	while(endRGB.length > 3) { endRGB.pop(); }
	var anim = new dojo.animation.Animation(
		new dojo.math.curves.Line(startRGB, endRGB),
		duration, 0);
	dojo.event.connect(anim, "onAnimate", function(e) {
		node.style.backgroundColor = "rgb(" + e.coordsAsInts().join(",") + ")";
	});
	if(callback) {
		dojo.event.connect(anim, "onEnd", function(e) {
			callback(node, anim);
		});
	}
	if( !dontPlay ) { anim.play(true); }
	return anim;
};

dojo.fx.html.wipeIn = function(node, duration, callback, dontPlay) {
	var savedHeight = dojo.html.getStyle(node, "height");
	var dispType = dojo.lang.inArray(node.tagName.toLowerCase(), ['tr', 'td', 'th']) ? "" : "block";
	node.style.display = dispType;
	var height = node.offsetHeight;
	var anim = dojo.fx.html.wipeInToHeight(node, duration, height, function(e) {
		node.style.height = savedHeight || "auto";
		if(callback) { callback(node, anim); }
	}, dontPlay);
};

dojo.fx.html.wipeInToHeight = function(node, duration, height, callback, dontPlay) {
	var savedOverflow = dojo.html.getStyle(node, "overflow");
	// FIXME: should we be setting display to something other than "" for the table elements?
	node.style.display = "none";
	node.style.height = 0;
	if(savedOverflow == "visible") {
		node.style.overflow = "hidden";
	}
	var dispType = dojo.lang.inArray(node.tagName.toLowerCase(), ['tr', 'td', 'th']) ? "" : "block";
	node.style.display = dispType;

	var anim = new dojo.animation.Animation(
		new dojo.math.curves.Line([0], [height]),
		duration, 0);
	dojo.event.connect(anim, "onAnimate", function(e) {
		node.style.height = Math.round(e.x) + "px";
	});
	dojo.event.connect(anim, "onEnd", function(e) {
		if(savedOverflow != "visible") {
			node.style.overflow = savedOverflow;
		}
		if(callback) { callback(node, anim); }
	});
	if( !dontPlay ) { anim.play(true); }
	return anim;
}

dojo.fx.html.wipeOut = function(node, duration, callback, dontPlay) {
	var savedOverflow = dojo.html.getStyle(node, "overflow");
	var savedHeight = dojo.html.getStyle(node, "height");
	var height = node.offsetHeight;
	node.style.overflow = "hidden";

	var anim = new dojo.animation.Animation(
		new dojo.math.curves.Line([height], [0]),
		duration, 0);
	dojo.event.connect(anim, "onAnimate", function(e) {
		node.style.height = Math.round(e.x) + "px";
	});
	dojo.event.connect(anim, "onEnd", function(e) {
		node.style.display = "none";
		node.style.overflow = savedOverflow;
		node.style.height = savedHeight || "auto";
		if(callback) { callback(node, anim); }
	});
	if( !dontPlay ) { anim.play(true); }
	return anim;
};

dojo.fx.html.explode = function(startNode, endNode, duration, callback) {
	var startCoords = [
		dojo.html.getAbsoluteX(startNode),
		dojo.html.getAbsoluteY(startNode),
		dojo.html.getInnerWidth(startNode),
		dojo.html.getInnerHeight(startNode)
	];
	return dojo.fx.html.explodeFromBox(startCoords, endNode, duration, callback);
};

// startCoords = [x, y, w, h]
dojo.fx.html.explodeFromBox = function(startCoords, endNode, duration, callback) {
	var outline = document.createElement("div");
	with(outline.style) {
		position = "absolute";
		border = "1px solid black";
		display = "none";
	}
	dojo.html.body().appendChild(outline);

	with(endNode.style) {
		visibility = "hidden";
		display = "block";
	}
	var endCoords = [
		dojo.html.getAbsoluteX(endNode),
		dojo.html.getAbsoluteY(endNode),
		dojo.html.getInnerWidth(endNode),
		dojo.html.getInnerHeight(endNode)
	];
	with(endNode.style) {
		display = "none";
		visibility = "visible";
	}

	var anim = new dojo.animation.Animation(
		new dojo.math.curves.Line(startCoords, endCoords),
		duration, 0
	);
	dojo.event.connect(anim, "onBegin", function(e) {
		outline.style.display = "block";
	});
	dojo.event.connect(anim, "onAnimate", function(e) {
		with(outline.style) {
			left = e.x + "px";
			top = e.y + "px";
			width = e.coords[2] + "px";
			height = e.coords[3] + "px";
		}
	});

	dojo.event.connect(anim, "onEnd", function() {
		endNode.style.display = "block";
		outline.parentNode.removeChild(outline);
		if(callback) { callback(endNode, anim); }
	});
	anim.play();
	return anim;
};

dojo.fx.html.implode = function(startNode, endNode, duration, callback) {
	var endCoords = [
		dojo.html.getAbsoluteX(endNode),
		dojo.html.getAbsoluteY(endNode),
		dojo.html.getInnerWidth(endNode),
		dojo.html.getInnerHeight(endNode)
	];
	return dojo.fx.html.implodeToBox(startNode, endCoords, duration, callback);
};

dojo.fx.html.implodeToBox = function(startNode, endCoords, duration, callback) {
	var outline = document.createElement("div");
	with(outline.style) {
		position = "absolute";
		border = "1px solid black";
		display = "none";
	}
	dojo.html.body().appendChild(outline);

	var anim = new dojo.animation.Animation(
		new dojo.math.curves.Line([
			dojo.html.getAbsoluteX(startNode),
			dojo.html.getAbsoluteY(startNode),
			dojo.html.getInnerWidth(startNode),
			dojo.html.getInnerHeight(startNode)
		], endCoords),
		duration, 0
	);
	dojo.event.connect(anim, "onBegin", function(e) {
		startNode.style.display = "none";
		outline.style.display = "block";
	});
	dojo.event.connect(anim, "onAnimate", function(e) {
		with(outline.style) {
			left = e.x + "px";
			top = e.y + "px";
			width = e.coords[2] + "px";
			height = e.coords[3] + "px";
		}
	});

	dojo.event.connect(anim, "onEnd", function() {
		outline.parentNode.removeChild(outline);
		if(callback) { callback(startNode, anim); }
	});
	anim.play();
	return anim;
};

dojo.fx.html.Exploder = function(triggerNode, boxNode) {
	var _this = this;

	// custom options
	this.waitToHide = 500;
	this.timeToShow = 100;
	this.waitToShow = 200;
	this.timeToHide = 70;
	this.autoShow = false;
	this.autoHide = false;

	var animShow = null;
	var animHide = null;

	var showTimer = null;
	var hideTimer = null;

	var startCoords = null;
	var endCoords = null;

	this.showing = false;

	this.onBeforeExplode = null;
	this.onAfterExplode = null;
	this.onBeforeImplode = null;
	this.onAfterImplode = null;
	this.onExploding = null;
	this.onImploding = null;

	this.timeShow = function() {
		clearTimeout(showTimer);
		showTimer = setTimeout(_this.show, _this.waitToShow);
	}

	this.show = function() {
		clearTimeout(showTimer);
		clearTimeout(hideTimer);
		//triggerNode.blur();

		if( (animHide && animHide.status() == "playing")
			|| (animShow && animShow.status() == "playing")
			|| _this.showing ) { return; }

		if(typeof _this.onBeforeExplode == "function") { _this.onBeforeExplode(triggerNode, boxNode); }
		animShow = dojo.fx.html.explode(triggerNode, boxNode, _this.timeToShow, function(e) {
			_this.showing = true;
			if(typeof _this.onAfterExplode == "function") { _this.onAfterExplode(triggerNode, boxNode); }
		});
		if(typeof _this.onExploding == "function") {
			dojo.event.connect(animShow, "onAnimate", this, "onExploding");
		}
	}

	this.timeHide = function() {
		clearTimeout(showTimer);
		clearTimeout(hideTimer);
		if(_this.showing) {
			hideTimer = setTimeout(_this.hide, _this.waitToHide);
		}
	}

	this.hide = function() {
		clearTimeout(showTimer);
		clearTimeout(hideTimer);
		if( animShow && animShow.status() == "playing" ) {
			return;
		}

		_this.showing = false;
		if(typeof _this.onBeforeImplode == "function") { _this.onBeforeImplode(triggerNode, boxNode); }
		animHide = dojo.fx.html.implode(boxNode, triggerNode, _this.timeToHide, function(e){
			if(typeof _this.onAfterImplode == "function") { _this.onAfterImplode(triggerNode, boxNode); }
		});
		if(typeof _this.onImploding == "function") {
			dojo.event.connect(animHide, "onAnimate", this, "onImploding");
		}
	}

	// trigger events
	dojo.event.connect(triggerNode, "onclick", function(e) {
		if(_this.showing) {
			_this.hide();
		} else {
			_this.show();
		}
	});
	dojo.event.connect(triggerNode, "onmouseover", function(e) {
		if(_this.autoShow) {
			_this.timeShow();
		}
	});
	dojo.event.connect(triggerNode, "onmouseout", function(e) {
		if(_this.autoHide) {
			_this.timeHide();
		}
	});

	// box events
	dojo.event.connect(boxNode, "onmouseover", function(e) {
		clearTimeout(hideTimer);
	});
	dojo.event.connect(boxNode, "onmouseout", function(e) {
		if(_this.autoHide) {
			_this.timeHide();
		}
	});

	// document events
	dojo.event.connect(document.documentElement || dojo.html.body(), "onclick", function(e) {
		if(_this.autoHide && _this.showing
			&& !dojo.dom.isDescendantOf(e.target, boxNode)
			&& !dojo.dom.isDescendantOf(e.target, triggerNode) ) {
			_this.hide();
		}
	});

	return this;
};

dojo.lang.mixin(dojo.fx, dojo.fx.html);
