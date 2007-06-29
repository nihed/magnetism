dojo.provide("dh.statistics.dataset");
dojo.require("dh.lang");

dh.statistics.dataset.Dataset = function() {
	this.numPoints = 0;
	this.ta = [];
	this.ya = [];
	this.minT = 0;
	this.maxT = 0;
	this.minY = 0;
	this.maxY = 0;
}

dh.lang.extend(dh.statistics.dataset.Dataset,
{
	add: function(t, y) {	
		this.ta[this.numPoints] = t;
		this.ya[this.numPoints] = y;
		
		if (this.numPoints == 0) {
			this.minT = this.maxT = t;
			this.minY = this.maxY = y;
		} else {	
			if (t < this.minT) 
				this.minT = t;
			if (t > this.maxT)
				this.maxT = t;
			if (y < this.minY)
				this.minY = y;
			if (y > this.maxY)
				this.maxY = y;				
		}
		
		this.numPoints++;
	},
	
	getIndexBelow: function(t) {
        var index = Math.floor(this.numPoints / 2);
        var interval = index;
        
        if (this.numPoints == 0 || t < this.ta[0])
        	return 0;
        
	    while (((index < this.numPoints - 1) && (t > this.ta[index+1])) || (t < this.ta[index])) {
	     	interval = Math.round(interval / 2);
	        if (t > this.ta[index]) {	        
	            index = index + interval; 
	        } else if (t < this.ta[index]) {
	            index = index - interval;
	        }
	    }  
	    
	    return index; 
	}
	
});

dh.statistics.dataset.createRandomCumulative = function() {
	var dataset = new dh.statistics.dataset.Dataset();

    var value = 0;
    for (var i = 0; i < 50; i++) {
        var r = Math.random();
        var increment = r * r * r;
        value += increment / 20;
        dataset.add(i, value);
    }
    
    return dataset;
}
	   
dh.statistics.dataset.createRandomWalk = function() {
	var dataset = new dh.statistics.dataset.Dataset();

	var value = 0;
    for (var i = 0; i < 50; i++) {
        var r = Math.random() - 0.5;
        var increment = r * r * r;
        value += increment;
	    if (value < 0)
	       value = 0;
        dataset.add(i, value);
    }

    return dataset;
}