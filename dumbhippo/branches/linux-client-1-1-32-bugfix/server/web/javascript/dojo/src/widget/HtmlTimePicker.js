dojo.provide("dojo.widget.HtmlTimePicker");
dojo.require("dojo.widget.HtmlWidget");
dojo.require("dojo.widget.TimePicker");

dojo.require("dojo.widget.*");
dojo.require("dojo.event.*");
dojo.require("dojo.html");

dojo.widget.HtmlTimePicker = function(){
	dojo.widget.TimePicker.call(this);
	dojo.widget.HtmlWidget.call(this);

	var _this = this;
	// selected time, JS Date object
	this.time = "";
	// set following flag to true if a default time should be set
	this.useDefaultTime = false;
	// rfc 3339 date
	this.storedTime = "";
	//this.storedTime = "";
	// time currently selected in the UI, stored in hours, minutes, seconds in the format that will be actually displayed
	this.currentTime = {};
	this.classNames = {
		selectedTime: "selectedItem"
	}
	// dom node indecies for selected hour, minute, amPm, and "any time option"
	this.selectedTime = {
		hour: "",
		minute: "",
		amPm: "",
		anyTime: false
	}

	this.templateCssPath = dojo.uri.dojoUri("src/widget/templates/HtmlTimePicker.css");

	// utility functions
	this.toRfcDateTime = function(jsDate) {
		if(!jsDate) {
			jsDate = this.today;
		}
		var year = jsDate.getFullYear();
		var month = jsDate.getMonth() + 1;
		if (month < 10) {
			month = "0" + month.toString();
		}
		var date = jsDate.getDate();
		if (date < 10) {
			date = "0" + date.toString();
		}
		var hour = jsDate.getHours();
		if (hour < 10) {
			hour = "0" + hour.toString();
		}
		var minute = jsDate.getMinutes();
		if (minute < 10) {
			minute = "0" + minute.toString();
		}
		// no way to set seconds, so set to zero
		var second = "00";
		var timeZone = jsDate.getTimezoneOffset();
		var timeZoneHour = parseInt(timeZone/60);
		if(timeZoneHour > -10 && timeZoneHour < 0) {
			timeZoneHour = "-0" + Math.abs(timeZoneHour);
		} else if(timeZoneHour < 10) {
			timeZoneHour = "+0" + timeZoneHour.toString();
		} else if(timeZoneHour >= 10) {
			timeZoneHour = "+" + timeZoneHour.toString();
		}
		var timeZoneMinute = timeZone%60;
		if(timeZoneMinute < 10) {
			timeZoneMinute = "0" + timeZoneMinute.toString();
		}
		return year + "-" + month + "-" + date + "T" + hour + ":" + minute + ":" + second + timeZoneHour +":" + timeZoneMinute;
	}

	this.fromRfcDateTime = function(rfcDate) {
		if(!rfcDate) {
			return new Date();
		}
		var tempTime = rfcDate.split("T")[1].split(":");
		// fullYear, month, date
		var tempDate = new Date();
		tempDate.setHours(tempTime[0]);
		tempDate.setMinutes(tempTime[1]);
		return tempDate;
	}

	this.toAmPmHour = function(hour) {
		var amPmHour = hour;
		var isAm = true;
		if (amPmHour == 0) {
			amPmHour = 12;
		} else if (amPmHour>12) {
			amPmHour = amPmHour - 12;
			isAm = false;
		} else if (amPmHour == 12) {
			isAm = false;
		}
		return [amPmHour, isAm];
	}

	this.fromAmPmHour = function(amPmHour, isAm) {
		var hour = parseInt(amPmHour, 10);
		if(isAm && hour == 12) {
			hour = 0;
		} else if (!isAm && hour<12) {
			hour = hour + 12;
		}
		return hour;
	}

	this.fillInTemplate = function(){
		this.initData();
		this.initUI();
	}

	this.initData = function() {
		// FIXME: doesn't currently validate the time before trying to set it
		// Determine the date/time from stored info, or by default don't 
		//  have a set time
		if(this.storedTime) {
			this.time = this.fromRfcDateTime(this.storedTime);
		} else if (this.useDefaultTime) {
			this.time = this.fromRfcDateTime();
		} else {
			this.selectedTime.anyTime = true;
		}
	}

	this.initUI = function() {
		// set UI to match the currently selected time
		if(this.time) {
			var amPmHour = this.toAmPmHour(this.time.getHours());
			var hour = amPmHour[0];
			var isAm = amPmHour[1];
			var minute = this.time.getMinutes();
			var minuteIndex = parseInt(minute/5);
			this.onSetSelectedHour(hour);
			this.onSetSelectedMinute(minuteIndex);
			this.onSetSelectedAmPm(isAm);
		}
	}

	this.setDateTime = function(rfcDate) {
		this.storedTime = rfcDate;
	}
	
	this.onClearSelectedHour = function(evt) {
		this.clearSelectedHour();
	}

	this.onClearSelectedMinute = function(evt) {
		this.clearSelectedMinute();
	}

	this.onClearSelectedAmPm = function(evt) {
		this.clearSelectedAmPm();
	}

	this.onClearSelectedAnyTime = function(evt) {
		this.clearSelectedAnyTime();
		if(this.selectedTime.anyTime) {
			this.selectedTime.anyTime = false;
			this.time = this.fromRfcDateTime();
			this.initUI();
		}
	}

	this.clearSelectedHour = function() {
		var hourNodes = this.hourContainerNode.getElementsByTagName("td");
		for (var i=0; i<hourNodes.length; i++) {
			dojo.html.setClass(hourNodes.item(i), "");
		}
	}

	this.clearSelectedMinute = function() {
		var minuteNodes = this.minuteContainerNode.getElementsByTagName("td");
		for (var i=0; i<minuteNodes.length; i++) {
			dojo.html.setClass(minuteNodes.item(i), "");
		}
	}

	this.clearSelectedAmPm = function() {
		var amPmNodes = this.amPmContainerNode.getElementsByTagName("td");
		for (var i=0; i<amPmNodes.length; i++) {
			dojo.html.setClass(amPmNodes.item(i), "");
		}
	}

	this.clearSelectedAnyTime = function() {
		dojo.html.setClass(this.anyTimeContainerNode, "anyTimeContainer");
	}

	this.onSetSelectedHour = function(evt) {
		this.onClearSelectedAnyTime();
		this.onClearSelectedHour();
		this.setSelectedHour(evt);
		this.onSetTime();
	}

	this.setSelectedHour = function(evt) {
		if(evt && evt.target) {
			dojo.html.setClass(evt.target, this.classNames.selectedTime);
			this.selectedTime["hour"] = evt.target.innerHTML;
		} else if (!isNaN(evt)) {
			var hourNodes = this.hourContainerNode.getElementsByTagName("td");
			if(hourNodes.item(evt)) {
				dojo.html.setClass(hourNodes.item(evt), this.classNames.selectedTime);
				this.selectedTime["hour"] = hourNodes.item(evt).innerHTML;
			}
		}
		this.selectedTime.anyTime = false;
	}

	this.onSetSelectedMinute = function(evt) {
		this.onClearSelectedAnyTime();
		this.onClearSelectedMinute();
		this.setSelectedMinute(evt);
		this.selectedTime.anyTime = false;
		this.onSetTime();
	}

	this.setSelectedMinute = function(evt) {
		if(evt && evt.target) {
			dojo.html.setClass(evt.target, this.classNames.selectedTime);
			this.selectedTime["minute"] = evt.target.innerHTML;
		} else if (!isNaN(evt)) {
			var minuteNodes = this.minuteContainerNode.getElementsByTagName("td");
			if(minuteNodes.item(evt)) {
				dojo.html.setClass(minuteNodes.item(evt), this.classNames.selectedTime);
				this.selectedTime["minute"] = minuteNodes.item(evt).innerHTML;
			}
		}
	}

	this.onSetSelectedAmPm = function(evt) {
		this.onClearSelectedAnyTime();
		this.onClearSelectedAmPm();
		this.setSelectedAmPm(evt);
		this.selectedTime.anyTime = false;
		this.onSetTime();
	}

	this.setSelectedAmPm = function(evt) {
		if(evt && evt.target) {
			dojo.html.setClass(evt.target, this.classNames.selectedTime);
			this.selectedTime["amPm"] = evt.target.innerHTML;
		} else if (!isNaN(evt)) {
			var amPmNodes = this.amPmContainerNode.getElementsByTagName("td");
			if(amPmNodes.item(evt)) {
				dojo.html.setClass(amPmNodes.item(evt), this.classNames.selectedTime);
				this.selectedTime["amPm"] = amPmNodes.item(evt).innerHTML;
			}
		}
	}

	this.onSetSelectedAnyTime = function(evt) {
		this.onClearSelectedHour();
		this.onClearSelectedMinute();
		this.onClearSelectedAmPm();
		this.setSelectedAnyTime();
		this.onSetTime();
	}

	this.setSelectedAnyTime = function(evt) {
		this.selectedTime.anyTime = true;
		dojo.html.setClass(this.anyTimeContainerNode, this.classNames.selectedTime + " " + "anyTimeContainer");
	}

	this.onClick = function(evt) {
		dojo.event.browser.stopEvent(evt)
	}

	this.onSetTime = function() {
		if(this.selectedTime.anyTime) {
			this.setDateTime();
		} else {
			var hour = 12;
			var minute = 0;
			var isAm = false;
			if(this.selectedTime["hour"]) {
				hour = parseInt(this.selectedTime["hour"], 10);
			}
			if(this.selectedTime["minute"]) {
				minute = parseInt(this.selectedTime["minute"], 10);
			}
			if(this.selectedTime["amPm"]) {
				isAm = (this.selectedTime["amPm"].toLowerCase() == "am");
			}
			this.time = new Date();
			this.time.setHours(this.fromAmPmHour(hour, isAm));
			this.time.setMinutes(minute);
			this.setDateTime(this.toRfcDateTime(this.time));
		}
	}

}
dojo.inherits(dojo.widget.HtmlTimePicker, dojo.widget.HtmlWidget);

dojo.widget.HtmlTimePicker.prototype.templateString = '<div class="timePickerContainer" dojoAttachPoint="timePickerContainerNode"><table class="timeContainer" cellspacing="0" ><thead><tr><td dojoAttachEvent="onClick: onSetSelectedHour;">Hour</td><td class="minutesHeading">Minute</td><td dojoAttachEvent="onClick: onSetSelectedHour;">&nbsp;</td></tr></thead><tbody><tr><td valign="top"><table><tbody dojoAttachPoint="hourContainerNode"  dojoAttachEvent="onClick: onSetSelectedHour;"><tr><td>12</td><td>6</td></tr> <tr><td>1</td><td>7</td></tr> <tr><td>2</td><td>8</td></tr> <tr><td>3</td><td>9</td></tr> <tr><td>4</td><td>10</td></tr> <tr><td>5</td><td>11</td></tr></tbody></table></td> <td valign="top" class="minutes"><table><tbody dojoAttachPoint="minuteContainerNode" dojoAttachEvent="onClick: onSetSelectedMinute;"><tr><td>00</td><td>30</td></tr> <tr><td>05</td><td>35</td></tr><tr><td>10</td><td>40</td></tr> <tr><td>15</td><td>45</td></tr> <tr><td>20</td><td>50</td></tr> <tr><td>25</td><td>55</td></tr></tbody></table> </td><td valign="top"><table><tbody dojoAttachPoint="amPmContainerNode" dojoAttachEvent="onClick: onSetSelectedAmPm;"><tr><td>AM</td></tr> <tr><td>PM</td></tr></tbody></table></td>																								</tr><tr><td></td><td><div dojoAttachPoint="anyTimeContainerNode" dojoAttachEvent="onClick: onSetSelectedAnyTime;" class="anyTimeContainer">any</div></td><td></td></tr></tbody></table></div>';
