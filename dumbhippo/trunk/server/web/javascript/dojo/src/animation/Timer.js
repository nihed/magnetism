dojo.provide("dojo.animation.Timer");

dojo.animation.Timer = function(intvl){
	var _this = this;
	var timer = null;
	this.isRunning = false;
	this.interval = intvl;

	this.onTick = function(){};
	this.onStart = null;
	this.onStop = null;

	this.setInterval = function(ms){
		if (this.isRunning) window.clearInterval(timer);
		this.interval = ms;
		if (this.isRunning) timer = window.setInterval(_this.onTick, _this.interval);
	};

	this.start = function(){
		if (typeof _this.onStart == "function") _this.onStart();
		this.isRunning = true;
		timer = window.setInterval(_this.onTick, _this.interval);
	};
	this.stop = function(){
		if (typeof _this.onStop == "function") _this.onStop();
		this.isRunning = false;
		window.clearInterval(timer);
	};
};
