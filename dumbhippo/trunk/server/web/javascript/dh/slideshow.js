dojo.provide("dh.slideshow");

var debug = function(message) {
	document.body.appendChild(document.createTextNode(message));
}

dh.slideshow.Slideshow = function(node, width, height, slides) {

	var me = this;
	this.node = node;
	this.slides = null;
	this.width = 0;
	this.height = 0;
	this.player = null;
	this.screen = null;
	this.current = -1;
	this.timer = null;
	this.playing = false;
	
	// internal util function
	var createElemWithClass = function(elem, parent, klass) {
		var d = document.createElement(elem);
		d.setAttribute("class", klass);
		parent.appendChild(d);
		return d;
	}
	
	var setText = function(node, text) {
		var t = document.createTextNode(text);
		while (node.firstChild) { node.removeChild(node.firstChild); }
		node.appendChild(t);
	}
	
	this.setSlides = function(slides) {
		
		// drop any old slides
		this.setCurrent(-1);
		this.setPlaying(false);
		
		if (this.screen) {
			while (this.screen.firstChild) {
				this.screen.removeChild(this.screen.firstChild);
			}
		}
		
		this.slides = slides;
		
		for (var i = 0; i < this.slides.length; ++i) {
				var slide = this.slides[i];
				if (!slide.node) {
					slide.node = document.createElement("img");
					slide.node.setAttribute("src", slide.src);
				}
				slide.node.setAttribute("style", "visibility: hidden;");
				// FIXME dojo.html.addClass probably
				slide.node.setAttribute("class", "dh-slideshow-slide");
				this.screen.appendChild(slide.node);
		}
	}
	
	this.setSize = function(width, height) {
		this.screen.style.width = width + "px";
		this.screen.style.height = height + "px";
	}
	
	var updateTimer = function() {
		if (me.playing) {
			if (!me.timer && (me.current + 1) < me.slides.length) {
				var slide = me.slides[me.current];
				me.timer = window.setTimeout(function() {
					me.setCurrent(me.current + 1);
					me.timer = null;
					updateTimer();
					if (!me.timer) {
						me.setPlaying(false);
					}
				}, slide.time);
			}
		} else {
			if (me.timer) {
				window.clearTimeout(me.timer);
				me.timer = null;
			}				
		}
	}

	this.updateControls = function() {
		if (this.playing) {				
			this.pauseControl.style.visibility = 'visible';
			this.playControl.style.visibility = 'hidden';
		} else {
			this.pauseControl.style.visibility = 'hidden';
			if ((this.current + 1) == this.slides.length)
				this.playControl.style.visibility = 'hidden';
			else
				this.playControl.style.visibility = 'visible';
		}
	}
	
	this.setPlaying = function(play) {
		if ((play && this.playing) || (!play && !this.playing))
			return;
		
		this.playing = play != false;
		
		if (this.playing) {
			if (this.current < 0)
				this.setCurrent(0);
		}
		updateTimer();
		this.updateControls();
	}
	
	this.setCurrent = function(slideIndex) {
	
		if (slideIndex < 0)
			slideIndex = -1;
		else if (slideIndex >= this.slides.length)
			slideIndex = this.slides.length - 1;
	
		if (this.current == slideIndex)
			return;
	
		var oldNode = null;
		if (this.current >= 0) {
			oldNode = this.slides[this.current].node;
		}

		this.current = slideIndex;
		if (this.current >= 0) {
			var slide = this.slides[slideIndex];
			this.current = slideIndex;
			
			slide.node.style.visibility = 'visible';
		}
		
		if (this.current >= 0) {
			setText(this.where, (this.current + 1) + "/" + this.slides.length);
		} else {
			setText(this.where, "-");
		}
		
		if (oldNode)
			oldNode.style.visibility = 'hidden';				
	}
	
	this.play = function() {
		this.setPlaying(true);
	}
	
	this.pause = function() {
		this.setPlaying(false);
	}
	
	this.restart = function() {
		this.setCurrent(0);
		this.setPlaying(true);
	}
	
	this.player = createElemWithClass("div", node, "dh-slideshow-player");
	
	// hide it until we get everything built
	this.player.style.display = 'none';
	
	this.screen = createElemWithClass("div", this.player, "dh-slideshow-screen");
	
	this.controlArea = createElemWithClass("div", this.player, "dh-slideshow-control-area");
	this.restartControl = createElemWithClass("a", this.controlArea, "dh-slideshow-control dh-slideshow-control-restart");
	this.pauseControl = createElemWithClass("a", this.controlArea, "dh-slideshow-control dh-slideshow-control-pause");
	this.playControl = createElemWithClass("a", this.controlArea, "dh-slideshow-control dh-slideshow-control-play");
	
	this.restartControl.onclick = function(ev) {
		me.restart();
	}
	
	this.pauseControl.onclick = function(ev) {
		me.pause();
	}
	
	this.playControl.onclick = function(ev) {
		me.play();
	}
	
	this.where = createElemWithClass("span", this.controlArea, "dh-slideshow-where");
	
	this.setSize(width, height);
	this.setSlides(slides);
	
	this.setCurrent(0);
	this.updateControls();
	
	// show everything
	this.player.style.display = 'block';
}
