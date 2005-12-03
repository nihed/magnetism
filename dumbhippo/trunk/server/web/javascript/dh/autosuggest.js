dojo.provide("dh.autosuggest");
/*******************************************************

AutoSuggest - a javascript automatic text input completion component
Copyright (C) 2005 Joe Kepley, The Sling & Rock Design Group, Inc.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*******************************************************

Please send any useful modifications or improvements via 
email to joekepley at yahoo (dot) com

*******************************************************/

/********************************************************
 The AutoSuggest class binds to a text input field
 and creates an automatic suggestion dropdown in the style
 of the "IntelliSense" and "AutoComplete" features of some
 desktop apps. 
 Parameters: 
 elem: A DOM element for an INPUT TYPE="text" form field
 suggestions: an array of strings to be used as suggestions
              when someone's typing.

 Example usage: 
 
 Please enter the name of a fruit.
 <input type="text" id="fruit" name="fruit" />
 <script language="Javascript">
 var fruits=new Array("apple","orange","grape","kiwi","cumquat","banana");
 new AutoSuggest(document.getElementById("fruit",fruits));
 </script>

 Requirements: 

 Unfortunately the AutoSuggest class doesn't seem to work 
 well with dynamically-created DIVs. So, somewhere in your 
 HTML, you'll need to add this: 
 <div id="dhAutoSuggest"><ul></ul></div>

 Here's a default set of style rules that you'll also want to 
 add to your CSS: 

 .dh-suggestion-list
 {
 background: white;
 border: 1px solid;
 padding: 4px;
 }

 .dh-suggestion-list ul
 {
 padding: 0;
 margin: 0;
 list-style-type: none;
 }

 .dh-suggestion-list a
 {
 text-decoration: none;
 color: navy;
 }

 .dh-suggestion-list .dh-selected
 {
 background: navy;
 color: white;
 }

 .dh-suggestion-list .dh-selected a
 {
 color: white;
 }

 #dhAutoSuggest
 {
 display: none;
 }
*********************************************************/
//counter to help create unique ID's
dh.autosuggest.idCounter = 0;

dh.autosuggest.AutoSuggest = function(elem)
{

	//The 'me' variable allow you to access the AutoSuggest object
	//from the elem's event handlers defined below.
	var me = this;

	//A reference to the element we're binding the list to.
	this.elem = elem;

	//The text input by the user.
	this.inputText = null;

	//A pointer to the index of the highlighted eligible item. -1 means nothing highlighted.
	this.highlighted = 0;

	//A div to use to create the dropdown.
	this.div = document.getElementById("dhAutoSuggest");

	//Do you want to remember what keycode means what? Me neither.
	var TAB = 9;
	var ESC = 27;
	var KEYUP = 38;
	var KEYDN = 40;
	var ENTER = 13;
	var SHIFT = 16;
	var CTRL = 17;
	var ALT = 18;
	var CAPS_LOCK = 20;

	//The browsers' own autocomplete feature can be problematic, since it will 
	//be making suggestions from the users' past input.
	//Setting this attribute should turn it off.
	elem.setAttribute("autocomplete","off");

	//We need to be able to reference the elem by id. If it doesn't have an id, set one.
	if(!elem.id)
	{
		var id = "autosuggest" + dh.autosuggest.idCounter;
		dh.autosuggest.idCounter++;

		elem.id = id;
	}

	/********************************************************
	onkeydown event handler for the input elem.
	Tab/Enter key = use the highlighted suggestion, if there is one.
	Esc key = get rid of the autosuggest dropdown
	Up/down arrows = Move the highlight up and down in the suggestions.
	********************************************************/
	elem.onkeydown = function(ev)
	{
		var key = me.getKeyCode(ev);

		switch(key)
		{
			case TAB:
			if (this.value.length == 0) {
				me.hideDiv();
				return;
			}
			var eligible = me.getEligible();
			//Go down to the next eligible
			if (me.highlighted < (eligible.length - 1))
			{
				me.highlighted++;
			}
			//Wrap around if we hit the end
			else if (me.highlighted > 0)
			{
				me.highlighted = 0;
			}
			//It's impossible to cancel the Tab key's default behavior. 
			//So this undoes it by moving the focus back to our field 
			//right after the event completes.
			setTimeout("document.getElementById('" + elem.id + "').focus()",0);
			me.changeHighlight(key);
			break;

			case ENTER:
			me.useSuggestion();
			break;

			case ESC:
			me.hideDiv();
			break;

			case KEYUP:
			if (me.highlighted > 0)
			{
				me.highlighted--;
			}
			me.changeHighlight(key);
			break;

			case KEYDN:
			var eligible = me.getEligible();
			if (me.highlighted < (eligible.length - 1))
			{
				me.highlighted++;
			}
			me.changeHighlight(key);
			break;
		}

	};

	/********************************************************
	onkeyup handler for the elem
	If the text is of sufficient length, and has been changed, 
	then display a list of eligible suggestions.
	********************************************************/
	elem.onkeyup = function(ev) 
	{
		var key = me.getKeyCode(ev);
		switch(key)
		{
		//The control keys were already handled by onkeydown, so do nothing.
		case TAB:
		case ENTER:
		case ESC:
		case KEYUP:
		case KEYDN:
			return;
		//Keys to generally ignore and leave the suggestions shown
		case CAPS_LOCK:
		case SHIFT:
		case CTRL:
		case ALT:
			return;
		default:
			me.checkUpdate(false);
		}
	};

	// see if we need to update the dropdown
	this.checkUpdate = function(eligibleChanged) {
		var update = false;
		if (me.elem.value != me.inputText && me.elem.value.length > 0)
		{
			update = true;
		}
		if (eligibleChanged)
			update = true;

		me.inputText = me.elem.value;

		if (update) {
			var eligible = me.getEligible();

			if (eligible.length > 0) {
				me.createDiv();
				me.positionDiv();
				me.showDiv();
			} else {
				me.hideDiv();
			}
		}
	};

	/********************************************************
	Insert the highlighted suggestion into the input box, and 
	remove the suggestion dropdown.
	********************************************************/
	this.useSuggestion = function()
	{
		var selectedId = this.getSelected();

		this.onSelected(selectedId);

		this.elem.value = ''; //this.eligible[this.highlighted];
		this.hideDiv();
	};
	
	this.getSelected = function() 
	{
		if (this.highlighted < 0)
			return null;
			
		var eligible = this.getEligible();
		return eligible[this.highlighted][1];
	};

	/********************************************************
	Display the dropdown. Pretty straightforward.
	********************************************************/
	this.showDiv = function()
	{
		this.div.style.display = 'block';
		this.highlighted = 0;
	};

	/********************************************************
	Hide the dropdown and clear any highlight.
	********************************************************/
	this.hideDiv = function()
	{
		this.div.style.display = 'none';
		this.highlighted = -1;
	};

	/********************************************************
	Modify the HTML in the dropdown to move the highlight.
	********************************************************/
	this.changeHighlight = function()
	{
		var lis = this.div.getElementsByTagName('LI');
		for (i in lis)
		{
			var li = lis[i];

			if ( ! li ) continue;

			if (this.highlighted == i)
			{
				li.className = "dh-selected";
			}
			else
			{
				li.className = "";
			}
		}
	};

	/********************************************************
	Position the dropdown div below the input text field.
	********************************************************/
	this.positionDiv = function()
	{
		var el = this.elem;
		var x = 0;
		var y = el.offsetHeight;
	
		//Walk up the DOM and add up all of the offset positions.
		while (el.offsetParent && el.tagName.toUpperCase() != 'BODY')
		{
			x += el.offsetLeft;
			y += el.offsetTop;
			el = el.offsetParent;
		}

		x += el.offsetLeft;
		y += el.offsetTop;

		this.div.style.left = x + 'px';
		this.div.style.top = y + 'px';
	};

	/********************************************************
	Build the HTML for the dropdown div
	********************************************************/
	this.createDiv = function()
	{
		var ul = document.createElement('ul');

		if (me.highlighted < 0) 
			me.highlighted = 0;

		//Append array of LI's to list for the matched words.
		var eligible = me.getEligible();
		for (i in eligible)
		{
			var li = eligible[i][0];

			if (! li ) continue;

			if (me.highlighted == i)
			{
				li.className = "dh-selected";
			}
	
			ul.appendChild(li);
		}

		this.div.replaceChild(ul,this.div.childNodes[0]);

		if (eligible.length == 0) {
			this.hideDiv();
		}	

		/********************************************************
		mouseover handler for the dropdown ul
		move the highlighted suggestion with the mouse
		********************************************************/
		ul.onmouseover = function(ev)
		{
			//Walk up from target until you find the LI.
			var target = me.getEventSource(ev);
			while (target.parentNode && target.tagName.toUpperCase() != 'LI')
			{
				target = target.parentNode;
			}
		
			var lis = me.div.getElementsByTagName('LI');
			
	
			for (i in lis)
			{
				var li = lis[i];
				if(target == li)
				{
					me.highlighted = i;
					break;
				}
			}
			me.changeHighlight();
		};

		/********************************************************
		click handler for the dropdown ul
		insert the clicked suggestion into the input
		********************************************************/
		ul.onclick = function(ev)
		{
			me.useSuggestion();
			me.hideDiv();
			me.cancelEvent(ev);
			setTimeout("document.getElementById('" + elem.id + "').focus()",0);
			return false;
		};
	
		this.div.className = "dh-suggestion-list";
		this.div.style.position = "absolute";

	};

	/********************************************************
	determine which of the suggestions matches the input
	********************************************************/
	this.getEligible = function()
	{
		// placeholder does nothing
		return new Array();
	};
	
	/********************************************************
	Function will be called upon selection of a completion element
	or on "activate" (enter is pressed), can be passed a null selectedId
	in that case
	********************************************************/
	this.setOnSelectedFunc = function(func)
	{
		this.onSelected = func;
	};
	/********************************************************
	Function must setup the eligible array	
	(i.e. this.eligible = new Array(); )
	********************************************************/
	this.setGetEligibleFunc = function(func)
	{
		this.getEligible = func;
	};
	/********************************************************
	Helper function to determine the keycode pressed in a 
	browser-independent manner.
	********************************************************/
	this.getKeyCode = function(ev)
	{
		if(ev)			//Moz
		{
			return ev.keyCode;
		}
		if(window.event)	//IE
		{
			return window.event.keyCode;
		}
	};

	/********************************************************
	Helper function to determine the event source element in a 
	browser-independent manner.
	********************************************************/
	this.getEventSource = function(ev)
	{
		if(ev)			//Moz
		{
			return ev.target;
		}
	
		if(window.event)	//IE
		{
			return window.event.srcElement;
		}
	};

	/********************************************************
	Helper function to cancel an event in a 
	browser-independent manner.
	(Returning false helps too).
	********************************************************/
	this.cancelEvent = function(ev)
	{
		if(ev)			//Moz
		{
			ev.preventDefault();
			ev.stopPropagation();
		}
		if(window.event)	//IE
		{
			window.event.returnValue = false;
		}
	};
}

