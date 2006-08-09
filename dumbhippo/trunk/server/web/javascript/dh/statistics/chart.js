dojo.provide("dh.statistics.chart");

dh.statistics.chart.Chart = function(width, height) {
	this.width = width;
	this.height = height;
	this.canvas = document.createElement("canvas");
	this.canvas.width = width;
	this.canvas.height = height;

	var context = this.canvas.getContext("2d");
	context.strokeStyle = "#60CC35";
}

dojo.lang.extend(dh.statistics.chart.Chart,
{
	getCanvas: function() {
		return this.canvas;
	},

	setDataset: function(dataset) {
		this.dataset = dataset;
		this._draw();
	},
	
	_draw: function() {
		var context = this.canvas.getContext("2d");

		// save the context and restore it on *all* exit points from the
		// function, otherwise end up applying values to previous translate 
		// and scale values consecutively, get funny flipping of the graph
		context.save();
		
		// move the origin to the bottom left corner
		context.translate(0, this.height);
		
	    // when you scale, shapes are drawn larger or smaller, 
	    // we are using -1 here because it flips y values, so that we can use
	    // positive y values on our graph plane
		context.scale(1, -1);
		
		context.clearRect(0, 0, this.width, this.height);
		
		if (this.dataset.numPoints == 0) {
            context.restore();
			return;
		}
		
		var xscale;
		// xscale determines how wide we need a single unit on the canvas to be to fit the 
		// whole range between minT and maxT
		if (this.dataset.maxT > this.dataset.minT)
			xscale = this.width / (this.dataset.maxT - this.dataset.minT);
		else
			xscale = 1;
					
		// our own reference point that will land minT in the bottom left corner	
		var x0 = - xscale * this.dataset.minT;
		
		var yscale;
		// yscale determines how high we need a single unit on the canvas to be to fit the 
		// whole range between minY and maxY	
		if (this.dataset.maxY > this.dataset.minY)
			yscale = this.height / (this.dataset.maxY - this.dataset.minY);
		else
			yscale = 1;
					
		// our own reference point that will land minY in the bottom left corner	
		var y0 = - yscale * this.dataset.minY;
		
		// "lift the pen" and move to (ta[0], ya[0]) on our scale
		context.moveTo(x0 + xscale * this.dataset.ta[0], y0 + yscale * this.dataset.ya[0])
		for (var i = 1; i < this.dataset.numPoints; i++) {
		    context.lineTo(x0 + xscale * this.dataset.ta[i], y0 + yscale * this.dataset.ya[i]);		    
		}
		
		context.stroke();
		
		context.restore();	
	}
});
