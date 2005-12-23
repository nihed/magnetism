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
	this.current = -2;
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
	
	var setImg = function(node, img) {
		while (node.firstChild) { node.removeChild(node.firstChild); }
		node.appendChild(img);		
	}

	var createImg = function(src) {
		var img = document.createElement("img");
			img.src = src;
		return img;
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
					slide.node.style.display = "none";
					slide.node.setAttribute("src", slide.src);
				}
				slide.node.style.display = "none";
				// FIXME dojo.html.addClass probably
				var clazz = slide.node.getAttribute("class");
				if (!clazz)
					clazz="";
				slide.node.setAttribute("class", clazz + " " + "dh-slideshow-slide");
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
		if (this.playing)
			setImg(this.playPauseControl, this.pauseImg);
		else
			setImg(this.playPauseControl, this.playImg);

		if (this.current < 1)
			this.back.disabled = true;
		else
			this.back.disabled = false;

		if (this.current >= this.slides.length - 1) {
			this.forward.disabled = true;
			this.playPauseControl.disabled = true;
		}
		else {
			this.forward.disabled = false;
			this.playPauseControl.disabled = false;
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
			
			slide.node.style.display = 'block';
		}

		if (this.current >= 0) {
			setText(this.where, (this.current + 1) + "/" + this.slides.length);

		} else {
			return;
		}
		
		this.updateControls();

		if (oldNode)
			oldNode.style.display = 'none';				
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
	// Fix for IE, since it's busted
	this.screen.style.border = "1px solid black";

	this.controlArea = createElemWithClass("div", this.player, "dh-slideshow-control-area");
	
	this.back = createElemWithClass("button", this.controlArea, "dh-slideshow-control dh-slideshow-control-back");
	setImg(this.back, createImg("/images/stock_media-prev.gif"));
	this.back.onclick = function(ev) {
		me.setCurrent(me.current - 1);
		me.setPlaying(false);
	}

	this.pauseImg = createImg("/images/stock_media-pause.gif");
	this.playImg = createImg("/images/stock_media-play.gif");

	this.playPauseControl = createElemWithClass("button", this.controlArea, "dh-slideshow-control dh-slideshow-control-pause");

	this.playPauseControl.onclick = function(ev) {
		if (me.playing)
			me.pause();
		else {
			me.setCurrent(me.current + 1);
			me.play();
		}
	}

	this.where = createElemWithClass("span", this.controlArea, "dh-slideshow-where");

	this.forward = createElemWithClass("button", this.controlArea, "dh-slideshow-control dh-slideshow-control-forward");
	setImg(this.forward, createImg("/images/stock_media-next.gif"));
	this.forward.onclick = function(ev) {
		me.setCurrent(me.current + 1);
		me.setPlaying(false);
	}
	
	this.setSize(width, height);
	this.setSlides(slides);
	
	this.setCurrent(0);
	this.updateControls();
	
	// show everything
	this.player.style.display = 'block';

	this.play();
}
