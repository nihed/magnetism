dojo.provide("dh.statistics.chart");

dh.statistics.chart.Chart = function(width, height) {
	this.width = width;
	this.height = height;
	this.canvas = document.createElement("canvas");
	this.canvas.width = width;
	this.canvas.height = height;
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
		
		context.clearRect(0, 0, this.width, this.height);
		
		if (this.dataset.numPoints == 0) {
			return;
		}
		
		context.translate(0, this.height);
		context.scale(1, - 1);
		
		var xscale;
		if (this.dataset.maxT > this.dataset.minT)
			xscale = this.width / (this.dataset.maxT - this.dataset.minT);
		else
			xscale = 1;
			
		var x0 = - xscale * this.dataset.minT;
		
		var yscale;
		if (this.dataset.maxY > this.dataset.minY)
			yscale = this.height / (this.dataset.maxY - this.dataset.minY);
		else
			yscale = 1;
			
		var y0 = - yscale * this.dataset.minY;
		
		context.moveTo(x0 + xscale * this.dataset.t[0], y0 + yscale * this.dataset.y[0])
		for (var i = 1; i < this.dataset.numPoints; i++)
		    context.lineTo(x0 + xscale * this.dataset.t[i], y0 + yscale * this.dataset.y[i]);
		    
		context.stroke();
	}
});
