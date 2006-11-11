dojo.provide("dh.statistics.selector");

dojo.require("dh.util");

dh.statistics.selector.DAYNAMES = [ "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" ]

dh.statistics.selector.Selector = function() {
   	this.table = document.createElement("table");
}
 
dojo.lang.extend(dh.statistics.selector.Selector,
{
	setRange: function(start, end) {
		if (this.startDate && this.startDate.getTime() == start && 
		    this.endDate && this.endDate.getTime() == end)
			return;

		var startDay = new Date(start);
		startDay.setHours(0,0,0,0);
	
		var startHour = new Date(0 + start);
		startHour.setMinutes(0,0,0);
	
		if (this.startDate && this.endDate) {
			var oldStartHourTime = this.startDate.getTime() - this.hourOffset;
			var oldNumHours = Math.ceil((this.endDate.getTime() - oldStartHourTime) / 3600000);

			var startHourTime = startHour.getTime();
			var numHours = Math.ceil((end - startHourTime) / 3600000);
	
			var hourChange = Math.round((startHourTime - oldStartHourTime) / 3600000);

			if (this._selectionStart != null) {
				this._selectionStart -= hourChange;
				this._selectionEnd -= hourChange;

				// If the old selection doesn't overlap the new range at all, remove it
				if (this._selectionEnd < 0 || this._selectionStart >= numHours) {
					this._selectionStart = null;
					this._selectionEnd = null;
				}
			}
		}
	
		this.startDate = new Date(start);
		this.endDate = new Date(end);
		
		this.dayOffset = start - startDay.getTime();
		this.hourOffset = start - startHour.getTime();
		
		this._update();
		this._updateSelection(this._selectionStart, this._selectionEnd);
		this._emitSelectionChanged();
	},
	
	getTable: function() {
		return this.table;
	},
	
	onSelectionChanged: function(start, end) {
	},

	_getDayIndex: function(date) {
		return Math.ceil((date.getTime() - this.startDate.getTime() + this.dayOffset) / 86400000);
	},
	
	_getHourIndex: function(date) {
		return Math.ceil((date.getTime() - this.startDate.getTime() + this.hourOffset) / 3600000);
	},
	
	_update: function() {
		if (this.tbody)
			this.table.removeChild(this.tbody);
		this.tbody = document.createElement("tbody");
		this.table.appendChild(this.tbody);
		
		this.hourCells = [];
		
		var dayNames = [];
		var dateNames = [];
		
		var numDays = this._getDayIndex(this.endDate);
		var numHours = this._getHourIndex(this.endDate);
	
		// Create a date object that we use to get the names for the days
		var date = new Date(Date.UTC(this.startDate.getFullYear(), this.startDate.getMonth(), this.startDate.getDate(), 12));
		for (var i = 0; i < numDays; i++) {
			dayNames[i] = dh.statistics.selector.DAYNAMES[date.getDay()];
			dateNames[i] = (1 + date.getMonth()) + "/" + (date.getDate());
			date.setHours(36,0,0,0);
		}
		
	    var headRow = document.createElement("tr");
		this.tbody.appendChild(headRow);
		for (var j = 0 ; j < numDays; j++) {
		    var th = document.createElement("th");
			th.className = "dh-hour dh-hour-head";
			th.appendChild(document.createTextNode(dayNames[j]));
			headRow.appendChild(th);
		}
	    headRow = document.createElement("tr");
		this.tbody.appendChild(headRow);
		for (var j = 0 ; j < numDays; j++) {
		    var th = document.createElement("th");
			th.className = "dh-hour dh-hour-head";
			th.appendChild(document.createTextNode(dateNames[j]));
			headRow.appendChild(th);
		}
		
		var hourIndexOffset = (this.dayOffset - this.hourOffset) / 3600000;
		
		for (var i = 0; i < 24; i++) {
		   var tr = document.createElement("tr");
		   this.tbody.appendChild(tr);
		   for (var j = 0 ; j < numDays; j++) {
		       var td = document.createElement("td");
			   td.appendChild(document.createTextNode(i));
			   var index = j * 24 + i - hourIndexOffset;
			   if (index < 0 || index >= numHours)
			   	   td.className = "dh-hour dh-disabled-hour";
			   else {
			       this.hourCells[index] = td;
			   	   td.className = "dh-hour dh-normal-hour";
		   	   }
			   td.addEventListener("mousedown", this._createMouseDownListener(), false);
		       tr.appendChild(td);
		   }
        }
	},
	
	_updateSelection: function(start, end) {
		for (var i = 0; i < this.hourCells.length; i++) {
			var td = this.hourCells[i];
			
			if (start != null && end != null && i >= start && i <= end)
		   	    td.className = "dh-hour dh-selected-hour";
  		    else
		   	    td.className = "dh-hour dh-normal-hour";
		}
	},
	
	_emitSelectionChanged: function() {
		var startTime = null;
		var endTime = null;

		// We don't clamp the reported selection to the range to avoid cycles
		// where fetch data, which results in a new range, which causes a 
		// selection change, which causes a fetch, and...
		if (this._selectionStart != null) {
			startTime = this.startDate.getTime() - this.hourOffset + (this._selectionStart * 3600000);
			endTime = this.startDate.getTime() - this.hourOffset + ((this._selectionEnd + 1) * 3600000);
		}

		this.onSelectionChanged(startTime, endTime);
	},
	
	_setSelection: function(start, end) {
		// Simplify by not allowing half-open selections
		if (start == null || end == null) {
			start = null;
			end = null;
		}
	
		this._selectionStart = start;
		this._selectionEnd = end;
		
		this._updateSelection(start, end);

		this._emitSelectionChanged();		
	},
	
	_startDrag: function(hourIndex) {
		if (hourIndex == null) {
			this._setSelection(null, null);
			return false;
		}
			
		this._dragAnchor = hourIndex;
		this._updateSelection(hourIndex, hourIndex);
		
		return true;
	},
	
	_updateDrag: function(hourIndex) {
		if (hourIndex == null) {
			this._updateSelection(this._selectionStart, this._selectionEnd);
			return;
		}
			
		var start = hourIndex < this._dragAnchor ? hourIndex : this._dragAnchor;
		var end = hourIndex > this._dragAnchor ? hourIndex : this._dragAnchor;
			
		this._updateSelection(start, end);
	},

	_endDrag: function(hourIndex) {
		if (hourIndex == null) {
			this._updateSelection(this._selectionStart, this._selectionEnd);
			return;
		}
			
		var start = hourIndex < this._dragAnchor ? hourIndex : this._dragAnchor;
		var end = hourIndex > this._dragAnchor ? hourIndex : this._dragAnchor;
			
		this._setSelection(start, end);
	},

	_findCell: function(e) {
		for (var i = 0; i < this.hourCells.length; i++) {
			if (this.hourCells[i] == e.target)
				return i;
		}
		
		return null;
	},
	
	_createMouseDownListener: function() {
		var me = this;
		
		function onMouseMove(e) {
			me._updateDrag(me._findCell(e));
			e.stopPropagation();
			e.preventDefault();
		}
		
		function onMouseUp(e) {
			window.removeEventListener("mousemove", onMouseMove, true);
			window.removeEventListener("mouseup", arguments.callee, true);
			me._endDrag(me._findCell(e));
			e.stopPropagation();
			e.preventDefault();
		}
		
		return function(e) {
			if (me._startDrag(me._findCell(e))) {
				window.addEventListener("mousemove", onMouseMove, true);
				window.addEventListener("mouseup", onMouseUp, true);
			}
			e.stopPropagation();
			e.preventDefault();
		};
	}
});
