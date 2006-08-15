dojo.provide("dh.statistics.chart");

dh.statistics.chart.Chart = function(width, height, coordinatesDivId) {
	this.width = width;
	this.height = height;
    this.coordinatesDiv = document.getElementById(coordinatesDivId);
	this.dataset = new dh.statistics.dataset.Dataset();
	this.xscale = 1;
	this.yscale = 1;
	this.x0 = 0;
	this.y0 = 0;
    this.oldX = null;	
	this.oldY = null;
	this.radius = null;
	this.canvas = document.createElement("canvas");
	this.canvas.width = width;
	this.canvas.height = height;
	
	// dojo.lang.hitch lets you associate a callback function with the scope of the member function
	this.canvas.addEventListener("mousemove", dojo.lang.hitch(this, "displayCoordinates"), false);

	var context = this.canvas.getContext("2d");
	context.strokeStyle = "#60CC35";
}

dojo.lang.extend(dh.statistics.chart.Chart,
{
	displayCoordinates: function(evt) {
	    var x = evt.clientX;
	    var y = evt.clientY; 
	    var canvasPos = dh.util.getBodyPosition(this.canvas);
        var canvasX = x - canvasPos.x;
        var canvasY = this.height - (y - canvasPos.y) - 1;
        dataX = (canvasX - this.x0) / this.xscale;
        dataY = (canvasY - this.y0) / this.yscale;
        if (this.dataset.numPoints >= 1) {
            var index = this.dataset.getIndexBelow(dataX);
            var ti = this.dataset.ta[index];
            var yi = this.dataset.ya[index];
            interpolatedY = yi;
            var xValue = dataX;
            var yValue = null;
            // TODO: coordinates don't match the points in the dataset exactly, 
            // so we should use xscale and yscale to evaluate how close the point is.
            // We then display the actual dataset point, because it is more interesting.
            var inDataset = false;
            if (index + 1 < this.dataset.numPoints) {
                var tj = this.dataset.ta[index+1];
                var yj = this.dataset.ya[index+1];
                interpolatedY = ((dataX - ti)*yj + (tj - dataX)*yi)/(tj - ti); 
                if ((Math.abs(dataX - tj) < 1000) && (interpolatedY.toFixed() == yj)) {
                    xValue = tj;
                    yValue = yj + ".00";
                    inDataset = true;
                }                                    
            } 
            
            if (!inDataset) {
                yValue = interpolatedY.toFixed(2);
                if ((Math.abs(dataX - ti) < 1000) && (interpolatedY.toFixed() == yi)) {
                    xValue = ti;
                    yValue = yi + ".00";    
                    inDataset = true;            
                }
            }
            
            this.coordinatesDiv.replaceChild(document.createTextNode("T: " + dh.util.timeString(xValue) + " Y: " + yValue), this.coordinatesDiv.firstChild);           
            this._highlightPoint(xValue, yValue, inDataset);
        }
	},

	getCanvas: function() {
		return this.canvas;
	},

	setDataset: function(dataset) {
		this.dataset = dataset;
		this._draw();
	},
	
	_draw: function(drawRegion) {
		var context = this.canvas.getContext("2d");

		// save the context and restore it on *all* exit points from the
		// function, otherwise end up applying values to previous translate 
		// and scale values consecutively, get funny flipping of the graph
		context.save();

		this._initializeContext(context);
		
        if (drawRegion) {
		     context.beginPath();
             context.arc(this.x0 + this.xscale*this.oldX, this.y0 + this.yscale*this.oldY, this.radius+1, 0, 2*Math.PI, false);
             context.clip();
        } else {
            this.oldX = null;
            this.oldY = null;
            this.radius = null;
        
		    // xscale determines how wide we need a single unit on the canvas to be to fit the 
		    // whole range between minT and maxT
		    if (this.dataset.maxT > this.dataset.minT)
			    this.xscale = this.width / (this.dataset.maxT - this.dataset.minT);
	  	    else
			    this.xscale = 1;

	        // our own reference point that will land minT in the bottom left corner	
		    this.x0 = - this.xscale * this.dataset.minT;		
		
		    // yscale determines how high we need a single unit on the canvas to be to fit the 
		    // whole range between minY and maxY	
		    if (this.dataset.maxY > this.dataset.minY)
			    this.yscale = this.height / (this.dataset.maxY - this.dataset.minY);
		    else
			    this.yscale = 1;
 		    
 		    // our own reference point that will land minY in the bottom left corner	
		    this.y0 = - this.yscale * this.dataset.minY;
	    }
		             
		context.clearRect(0, 0, this.width, this.height);
		
		if (this.dataset.numPoints == 0) {
            context.restore();
			return;
		}
		
		// "lift the pen" and move to (ta[0], ya[0]) on our scale
		context.moveTo(this.x0 + this.xscale * this.dataset.ta[0], this.y0 + this.yscale * this.dataset.ya[0])
		for (var i = 1; i < this.dataset.numPoints; i++) {
		    context.lineTo(this.x0 + this.xscale * this.dataset.ta[i], this.y0 + this.yscale * this.dataset.ya[i]);		    
		}
		
		context.stroke();
		
		context.restore();	
	},
	
	_highlightPoint: function(x, y, inDataset) {
	    if (this.radius != null)
            this._draw(true);
        
		var context = this.canvas.getContext("2d");

		context.save();        			
		
		this._initializeContext(context);

        context.beginPath();
	    if (inDataset) {
	        context.fillStyle = "#FF0000";	    
  		    this.radius = 5;
	    } else {
	        context.fillStyle = "#FF6600";   
	        this.radius = 3;
	    }    
  		context.arc(this.x0 + this.xscale*x, this.y0 + this.yscale*y, this.radius, 0, 2*Math.PI, false);
	    context.fill();    

		this.oldX = x;
		this.oldY = y;
		context.restore();	
	},
	
	_initializeContext: function(context) {
	    // move the origin to the bottom left corner
		context.translate(0, this.height);
		
	    // when you scale, shapes are drawn larger or smaller, 
	    // we are using -1 here because it flips y values, so that we can use
	    // positive y values on our graph plane
		context.scale(1, -1);	
	}
});
