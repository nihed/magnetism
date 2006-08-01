dojo.provide("dh.statistics.dataset");

dh.statistics.dataset.Dataset = function() {
	this.numPoints = 0;
	this.t = [];
	this.y = [];
	this.minT = 0;
	this.maxT = 0;
	this.minY = 0;
	this.maxY = 0;
}

dojo.lang.extend(dh.statistics.dataset.Dataset,
{
	add: function(t, y) {
		this.t[this.numPoints] = t;
		this.y[this.numPoints] = y;
		
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