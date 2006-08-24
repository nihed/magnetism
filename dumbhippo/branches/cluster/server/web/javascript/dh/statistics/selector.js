dojo.provide("dh.statistics.selector");

dh.statistics.selector.DAYNAMES = [ "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" ]

dh.statistics.selector.Selector = function() {
   	this.table = document.createElement("table");
}
 
dojo.lang.extend(dh.statistics.selector.Selector,
{
	setRange: function(startDate, endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
		
		var startDay = new Date(startDate.getTime());
		startDay.setHours(0,0,0,0);
		this.dayOffset = startDate.getTime() - startDay.getTime();
		
		var startHour = new Date(startDate.getTime());
		startHour.setMinutes(0,0,0);
		this.hourOffset = startDate.getTime() - startHour.getTime();
		
		this._update()
	},
	
	getTable: function() {
		return this.table;
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
			   if (index < 0 || index > numHours)
			   	   td.className = "dh-hour dh-disabled-hour";
			   else
			   	   td.className = "dh-hour dh-normal-hour";
		       tr.appendChild(td);
		       this.hourCells[index] = td;
		   }
        }
	}
});
