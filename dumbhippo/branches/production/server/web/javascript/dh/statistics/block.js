dojo.provide("dh.statistics.block");
dojo.require("dh.statistics.chart");

dh.statistics.block.Block = function() {
	this._set = null;
	this._chart = new dh.statistics.chart.Chart(600, 200, "dhCoords1");
	
	this._table = document.createElement("table");
	this._table.className = "dh-block-table";
	var tbody = document.createElement("tbody");
	this._table.appendChild(tbody);
	
	var tr, td, div;
	
	///// Selectors
	
	tr = document.createElement("tr");
	tbody.appendChild(tr);
	
	td = document.createElement("td");
	tr.appendChild(td);

	td = document.createElement("td");
	tr.appendChild(td);
	td.className = "dh-column-select";
	this._columnSelect = document.createElement("select");
	td.appendChild(this._columnSelect);
	
	td = document.createElement("td");
	tr.appendChild(td);
	td.className = "dh-server-select";

	this._remove = document.createElement("div");
	td.appendChild(this._remove);
	this._remove.className = "dh-remove";
	this._remove.appendChild(document.createTextNode("X"));
	
	this._serverSelect = document.createElement("select");
	td.appendChild(this._serverSelect);
	
	//// Main row
	
	tr = document.createElement("tr");
	tbody.appendChild(tr);

	td = document.createElement("td");
	tr.appendChild(td);
	td.className = "dh-max-val";
	this._maxValDiv = document.createElement("div");
	td.appendChild(this._maxValDiv);
	
	td = document.createElement("td");
	td.className = "dh-graph";
	td.colSpan = 2;
	td.rowSpan = 2;
	tr.appendChild(td);
	div = document.createElement("div");
	td.appendChild(div);
	div.appendChild(this._chart.getCanvas());	
	
	/// Row for minimum value
	
	tr = document.createElement("tr");
	tbody.appendChild(tr);
	
	td = document.createElement("td");
	tr.appendChild(td);
	td.className = "dh-min-val";
	this._minValDiv = document.createElement("div");
	td.appendChild(this._minValDiv);
	
	/// Timescale row
	
	tr = document.createElement("tr");
	tbody.appendChild(tr);

	td = document.createElement("td");
	tr.appendChild(td);
	
	td = document.createElement("td");
	tr.appendChild(td);
	td.className = "dh-start-time";
	this._startTimeDiv = document.createElement("div");
	td.appendChild(this._startTimeDiv);
	
	td = document.createElement("td");
	tr.appendChild(td);
	this._endTimeDiv = document.createElement("div");
	td.className = "dh-end-time";
	td.appendChild(this._endTimeDiv);
	
	// Coordinates row
	
	tr = document.createElement("tr");
	tbody.appendChild(tr);

	td = document.createElement("td");
	tr.appendChild(td);

	td = document.createElement("td");
	tr.appendChild(td);
	td.className = "dh-coordinates";
	td.colSpan = 2;
	this._coordinatesDiv = document.createElement("div");
	td.appendChild(this._coordinatesDiv);
	
	this._connectEvents();
}

dojo.lang.extend(dh.statistics.block.Block,
{
	getTable: function() {
		return this._table;
	},

	setRange: function(minT, maxT) {
		this._chart.setRange(minT, maxT);
		this._updateAxes();
	},

	setSet: function(set) {
		this._set = set;
		this._fillColumnSelect();
		this._fillServerSelect();
		this.onSpecificationChanged();
	},
	
	getSpecification: function() {
		if (!this._set)
			return null;
	
		var filename = this._set.filename;
		var server = this._serverSelect.value;
		var column = this._columnSelect.value;
		
		return new dh.statistics.fetcher.Specification(server, filename, column);
	},

	setDataset: function(dataset) {
		this._chart.setDataset(dataset);
		this._updateAxes();
	},

	onSpecificationChanged: function() {
	},

	onRemove: function() {
	},

	_fillColumnSelect: function() {
		var oldColumn = this._columnSelect.value;
	
		this._columnSelect.options.length = 0;
		for (var i = 0; i < this._set.length; i++) {
			var column = this._set.item(i);
			var defaultSelected = i == 0;
			var selected = oldColumn ? column.id == oldColumn : defaultSelected;
		
			this._columnSelect.options[i] = 
				new Option(column.name, column.id, defaultSelected, selected);
		}
	},
	
	_fillServerSelect: function() {
		var oldServer = this._serverSelect.value;
		var servers = dh.statistics.servers;
		var thisServer = dh.statistics.thisServer;
		
		// Only allow selection of server for the current set
		if (this._set.current) {
			this._serverSelect.disabled = false;
			for (var i = 0; i < servers.length; i++) {
				var defaultSelected = servers[i] == thisServer;
				var selected = oldServer ? servers[i] == oldServer : defaultSelected;
				this._serverSelect.options[i] = new Option(servers[i],
			    	                                       servers[i],
			        	                                   defaultSelected,
			            	                               selected);
			}
		} else {
			this._serverSelect.disabled = true
			this._serverSelect.options[0] = new Option(thisServer,
				   								       thisServer,
												       true,
												       true);
		 }
	},
	
	_setText: function(div, text) {
		var textNode = document.createTextNode(text);
		if (div.firstChild)
			div.replaceChild(textNode, div.firstChild);
		else
			div.appendChild(textNode);
	},
	
	_updateAxes: function() {
		var dataset = this._chart.dataset;
		var minT = this._chart.minT != null ? this._chart.minT : dataset.minT;
		var maxT = this._chart.maxT != null ? this._chart.maxT : dataset.maxT;
	    
	    var startTime = minT == 0 ? "" : dh.util.timeString(minT);
	    this._setText(this._startTimeDiv, startTime);
	    
		var endTime = (minT == maxT) ? "" : dh.util.timeString(maxT);
		this._setText(this._endTimeDiv, endTime);
		
	    // this will display 0 next to the origin point if there is no data in the dataset
	    this._setText(this._minValDiv, dataset.minY);

		var maxVal = (dataset.minY == dataset.maxY) ? "" : dataset.maxY;
		this._setText(this._maxValDiv, maxVal);
	},
	
	_connectEvents: function() {
		var me = this;
	
		dh.util.addEventListener(this._serverSelect, "change", function() {
			me.onSpecificationChanged();
		});
		
		dh.util.addEventListener(this._columnSelect, "change", function() {
			me.onSpecificationChanged();
		});

		this._chart.onCoordinatesChanged = function(t, y) {
			var text;
			if (t != null && y != null)
				text = "T: " + dh.util.timeString(t) + " Y: " + y;
			else
				text = "";
			me._setText(me._coordinatesDiv, text);
		};
		
		dh.util.addEventListener(this._remove, "mouseover", function() {
			me._remove.className = "dh-remove dh-remove-prelight";
		});
		dh.util.addEventListener(this._remove, "mouseout", function() {
			me._remove.className = "dh-remove";
		});
		dh.util.addEventListener(this._remove, "mousedown", function() {
			me.onRemove();
		});
	}
});
