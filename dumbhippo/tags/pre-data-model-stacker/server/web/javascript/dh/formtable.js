dojo.provide("dh.formtable")
dojo.require("dh.lang")

dh.formtable.undoValues = {};
dh.formtable.currentValues = null; // gets filled in as part of the jsp
dh.formtable.expandableTextInputs = {};

dh.formtable.linkClosures = {};
dh.formtable.invokeLinkClosure = function(which) {
	var f = dh.formtable.linkClosures[which];
	f.call(this);
}

dh.formtable.linkClosuresCount = 0;

dh.formtable.makeLinkClosure = function(f) {
	dh.formtable.linkClosures[dh.formtable.linkClosuresCount] = f;
	var link = "javascript:dh.formtable.invokeLinkClosure(" + dh.formtable.linkClosuresCount + ");";
	dh.formtable.linkClosuresCount = dh.formtable.linkClosuresCount + 1;
	return link;
}

dh.formtable.getStatusRow = function(controlId) {
	return document.getElementById(controlId + 'StatusRow');
}

dh.formtable.getStatusSpacer = function(controlId) {
	return document.getElementById(controlId + 'StatusSpacer');
}

dh.formtable.getStatusText = function(controlId) {
	return document.getElementById(controlId + 'StatusText');
}

dh.formtable.getStatusLink = function(controlId, order) {
	return document.getElementById(controlId + 'StatusLink' + order);
}

dh.formtable.hideStatus = function(controlId) {
	var statusRow = dh.formtable.getStatusRow(controlId);
	var statusSpacer = dh.formtable.getStatusSpacer(controlId);
	statusRow.style.display = 'none';
	statusSpacer.style.display = 'none';
}

dh.formtable.showStatus = function(controlId, statusText, linkTexts, linkHrefs, linkTitles) {
	var statusRow = dh.formtable.getStatusRow(controlId);
	var statusSpacer = dh.formtable.getStatusSpacer(controlId);
	var statusTextNode = dh.formtable.getStatusText(controlId);
	dh.dom.textContent(statusTextNode, statusText);
	
	var statusLinkNode = null;
	var i = 0;	
	while (i < linkTexts.length) {
	    statusLinkNode = dh.formtable.getStatusLink(controlId, i+1);	
		dh.dom.textContent(statusLinkNode, linkTexts[i]);
		statusLinkNode.href = linkHrefs[i];
		statusLinkNode.title = linkTitles[i];
		statusLinkNode.style.display = 'inline';
		i = i + 1;
	}	

	while (true) {
	    statusLinkNode = dh.formtable.getStatusLink(controlId, i+1);
	    if (statusLinkNode == null)
	        break;
	    statusLinkNode.style.display = 'none';
		i = i + 1;	        
	}
	
	// Show everything
	try {
		// Firefox
		statusRow.style.display = 'table-row';
		statusSpacer.style.display = 'table-row';
	} catch (e) {
		// IE
		statusRow.style.display = 'block';
		statusSpacer.style.display = 'block';
	}
}

dh.formtable.showStatusMessage = function(controlId, message, hideClose) {
	dh.formtable.showStatus(controlId, message, hideClose ? [] : ["Close"],
		hideClose ? [] : [dh.formtable.makeLinkClosure(function() {
			dh.formtable.hideStatus(controlId);
		})],
		hideClose ? [] : ["I've read this already, go away"]);
}

dh.formtable.initExpanded = function (controlId, labelExpand) {
	var container = document.getElementById(controlId + "FormContainer");
	container.dhExpandedLabel = labelExpand;
	dh.formtable.setExpandedEditing(controlId, false);
	dh.formtable.setExpandedError(controlId, false);
}

dh.formtable.setExpandedEditing = function (controlId, active) {
	var container = document.getElementById(controlId + "FormContainer");
	var label = document.getElementById(controlId + "FormLabel");
	var content = document.getElementById(controlId + "FormContent");
	if (active) {
		dh.util.prependClass(container, "dh-account-editing-box");
		if (container.dhExpandedLabel) {		
			dh.util.prependClass(label.parentNode, "dh-account-editing-label-box");
			dh.util.prependClass(label, "dh-account-editing");
			dh.util.prependClass(content.parentNode, "dh-account-editing-content-box");			
		} else {
			dh.util.prependClass(content.parentNode, "dh-account-editing-content-only-box");
		}
		dh.util.prependClass(content, "dh-account-editing");		
	} else {	
		dh.util.removeClass(container, "dh-account-editing-box");	
		if (container.dhExpandedLabel) {			
			dh.util.removeClass(label.parentNode, "dh-account-editing-label-box");	
			dh.util.removeClass(label, "dh-account-editing");
			dh.util.removeClass(content.parentNode, "dh-account-editing-content-box");
		} else {
			dh.util.removeClass(content.parentNode, "dh-account-editing-content-only-box");
		}
		dh.util.removeClass(content, "dh-account-editing");		
	}		
}
	
dh.formtable.setExpandedError = function(controlId, active) {
	var label = document.getElementById(controlId + "FormLabel");
	var content = document.getElementById(controlId + "FormContent");
	if (active) {
		dh.util.removeClass(label.parentNode, "dh-account-editing-box-success");		
		dh.util.removeClass(content.parentNode, "dh-account-editing-box-success");			
		dh.util.prependClass(label.parentNode, "dh-account-editing-box-error");		
		dh.util.prependClass(content.parentNode, "dh-account-editing-box-error");	
	} else {
		dh.util.removeClass(label.parentNode, "dh-account-editing-box-error");		
		dh.util.removeClass(content.parentNode, "dh-account-editing-box-error");				
		dh.util.prependClass(label.parentNode, "dh-account-editing-box-success");		
		dh.util.prependClass(content.parentNode, "dh-account-editing-box-success");			
	}
}

dh.formtable._doChange = function(controlId, isXmlMethod, methodName, argName, value, fixedArgs,
                                  onSuccess, onFailed) {
	var args;
	if (fixedArgs != null)
		args = dh.lang.shallowCopy(fixedArgs)
	else
		args = {}
	args[argName] = value;
	                                  
	if (isXmlMethod)
		invokeMethod = dh.server.doXmlMethod;
	else
		invokeMethod = dh.server.doPOST;
   	invokeMethod (methodName,
			      args,
		  	      function(/*type, data, http -or- childNodes, http*/) {
					onSuccess();
					// for the expandable text inputs, we keep the old 
					// value until the input is being edited again
					if (dh.formtable.expandableTextInputs[controlId] == null) {
 	    	 		    // this saves what we'll undo to after the NEXT change
					    dh.formtable.undoValues[controlId] = value;
					}
	  	    	  },
			  	  function(arg1, arg2, arg3 /*type, error, http -or- code, msg, http */) {
			  	  	if (isXmlMethod)
			  	      onFailed(arg2);
			  	    else 
			  	      onFailed();
			  	  });                            
}

dh.formtable._onValueChanged = function(entryObject, isXmlMethod, postMethod, argName, value,
 									   pendingMessage, successMessage, fixedArgs, onUpdate) {
	var controlId = entryObject.elem.id;										

	dh.formtable.showStatus(controlId, pendingMessage, {}, {}, {});

	dh.formtable._doChange(controlId, isXmlMethod, postMethod, argName, value, fixedArgs,
	                       function () {
	                         var oldValue = dh.formtable.undoValues[controlId];
  				  	    	 dh.formtable.showStatus(controlId, successMessage,
									  	    		 oldValue ? ["Undo"] : [],
									  	    	 	 oldValue ?
				  	    	                          [dh.formtable.makeLinkClosure(function() {
									  	    			dh.formtable.undo(entryObject, oldValue, postMethod, argName, fixedArgs, onUpdate);
				  							    		})] : [],
										  	    	 oldValue ? ["Change back to the previous setting"] : []);
							 if (onUpdate != null) {
						 		 onUpdate(value);
							 }
						   },
						   function (errMsg) {
			  	    		 var msg = "Failed to save this setting.";
			  	    		 if (errMsg)
			  	    		   msg = msg + " (" + errMsg + ")";
							dh.formtable.showStatusMessage(controlId, msg);
			  	    	   });      						   								  	    	
}

dh.formtable.onValueChanged = function(entryObject, postMethod, argName, value,
 									   pendingMessage, successMessage, fixedArgs, onUpdate) {
	dh.formtable._onValueChanged(entryObject, false, postMethod, argName, value, pendingMessage, successMessage, fixedArgs, onUpdate);
}

dh.formtable.onValueChangedXmlMethod = function(entryObject, postMethod, argName, value,
 									   pendingMessage, successMessage, fixedArgs, onUpdate) {
	dh.formtable._onValueChanged(entryObject, true, postMethod, argName, value, pendingMessage, successMessage, fixedArgs, onUpdate);
}

dh.formtable.undo = function(entryObject, oldValue, postMethod, argName, fixedArgs, onUpdate) {
	var controlId = entryObject.elem.id;
	dh.formtable.showStatus(controlId, "Undoing...", {}, {}, {});
	var args
	if (fixedArgs != null)
		args = dh.lang.shallowCopy(fixedArgs)
	else
		args = {}
	args[argName] = oldValue;
   	dh.server.doPOST(postMethod,
			     		args,
		  	    		function(type, data, http) {
		  	    			entryObject.setValue(oldValue, true); // true = doesn't emit the changed signal
		  	    	 		dh.formtable.showStatusMessage(controlId, "Undone!");
	 					  	// this saves what we'll undo to after the NEXT change
							dh.formtable.undoValues[controlId] = oldValue;
							if (onUpdate != null)
								onUpdate(oldValue)
			  	    	},
			  	    	function(type, error, http) {
							dh.formtable.showStatus(controlId, "Failed to undo!", {}, {}, {});
			  	    	});	
}

dh.formtable.ExpandableTextInput = function(controlId, defaultVal) {
	this._controlId = controlId;
	
	var me = this;
	
	dh.formtable.initExpanded(controlId, false);

	this._input = new dh.textinput.Entry(document.getElementById(controlId), defaultVal, dh.formtable.currentValues[controlId]);
	this._input.timeoutBeforeChange = 200;
	
	dh.formtable.undoValues[controlId] = this._input.getValue();		
	dh.formtable.expandableTextInputs[controlId] = this;
	
	this._saveLink = null;	
	this._saveFunc = null;	
	
	this._handlingFocus = false;
	
	this._input.onfocus = function () {
		me._handlingFocus = true;
	    dh.formtable.undoValues[controlId] = me._input.getValue();
		me._setStatus('edited');
		dh.formtable.setExpandedEditing(controlId, true);
		me._descriptionNode.style.display = "block";
		me._handlingFocus = false;
	}

    this._input.onkeyup = function () {
        me._updateSaveLink();
    }
    
    // this checks if the input value has changed and the save link 
    // needs to look active
    this._updateSaveLink = function () {
        if (me._input.getValue() != me._input.lastValue) {
            if (!dh.util.hasClass(me._saveLink, "dh-placebo-active-link")) {               
			    dh.util.prependClass(me._saveLink, "dh-placebo-active-link");		   
			}             
        } else {
            if (dh.util.hasClass(me._saveLink, "dh-placebo-active-link")) {
			    dh.util.removeClass(me._saveLink, "dh-placebo-active-link");		   
			}              
        }
    }       
    
    this._input.onValueChanged = function () {
		me._setStatus('saving');
		me._saveFunc(function () { me._setStatus('saved'); },
		             function (msg) { me._setStatus('error', msg); });    
    }
    	
	this._descriptionNode = document.getElementById(controlId + "Description");
	
	this._statusLink = document.createElement("span");
	this._descriptionNode.appendChild(this._statusLink);
	
	this._setStatus = function(status, msg) {
		dh.util.clearNode(me._statusLink);
		var child;
		var child2;
		dh.formtable.setExpandedError(controlId, false);
		if (status == 'edited') {
		    me._input.cancelEdit = false;
			child = document.createElement("span");
			child.appendChild(document.createTextNode("Save"));
			dh.util.prependClass(child, "dh-placebo-link");		
			me._saveLink = child;
			var cancelAction = function() {		
			    expandableTextInput = dh.formtable.expandableTextInputs[controlId];	 
			    expandableTextInput._input.cancelEdit = true;
			    expandableTextInput._descriptionNode.style.display = "none";
			    dh.formtable.setExpandedEditing(controlId, false);
			    expandableTextInput._input.setValue(dh.formtable.undoValues[controlId], true);
		    }		    
			child2 = dh.util.createActionLinkElement("Cancel", cancelAction, "dh-active-link", "");     	
		} else if (status == 'saving') {
			child = document.createElement("span");
			child.appendChild(document.createTextNode("Saving..."));
			dh.util.prependClass(child, "dh-saved-link");
		} else if (status == 'saved') {
			child = document.createElement("span");
			var img = document.createElement("img");
			img.setAttribute("src", dhImageRoot3 + "check.png");
			img.style.marginLeft = "3px";			
			img.style.marginRight = "3px";
			img.style.verticalAlign = "baseline";
			child.appendChild(img);		
			child.appendChild(document.createTextNode("Saved"));
			dh.util.prependClass(child, "dh-saved-link")
			var undoAction = function() {			 
			    undoValue = dh.formtable.undoValues[controlId];
			    // elem.focus() seemed to work asynchronously, which would cause 
			    // things below to be done out of order, so we do all the preparation
			    // work in our own handleFocus(), and then call elem.focus() 
			    // to get the cursor into the box 
			    dh.formtable.expandableTextInputs[controlId]._input.handleFocus();			    	    
			    dh.formtable.expandableTextInputs[controlId]._input.setValue(undoValue, true);
			    // dh.formtable.undoValues[controlId] is now the value that was saved and that we are undoing
			    dh.formtable.expandableTextInputs[controlId]._input.lastValue = dh.formtable.undoValues[controlId];
			    dh.formtable.expandableTextInputs[controlId]._updateSaveLink();
			    dh.formtable.expandableTextInputs[controlId]._input.elem.focus();			    
		    }		    
			child2 = dh.util.createActionLinkElement("Undo", undoAction, "dh-active-link", "");    			
		} else if (status == 'error') {
			child = document.createElement("span");
			var span = document.createElement("span");
			span.appendChild(document.createTextNode("Error"));
			if (msg)
				span.appendChild(document.createTextNode(": "));
			dh.util.prependClass(child, "dh-account-error-prefix");
			child.appendChild(span)
			if (msg)
				span.appendChild(document.createTextNode(msg));
			dh.formtable.setExpandedError(controlId, true);
		}
		me._statusLink.appendChild(child);
		if (child2)
		    me._statusLink.appendChild(child2);
	}
	
	this._setStatus('edited');
	
	this.setDescription = function (desc) {
		this.setContent(document.createTextNode(desc));
	}
	
	this.setContent = function (content) {
		this._descriptionNode.innerHTML = "";
		this._descriptionNode.appendChild(content);
		this._descriptionNode.appendChild(document.createTextNode(" "));
		this._descriptionNode.appendChild(me._statusLink);
	}
	
	this.setChangedPost = function(methodName, argName, fixedArgs) {
		me._saveFunc = function (onSuccess, onFailure) {	
            dh.formtable._doChange(controlId, false, methodName, argName, me._input.getValue(), fixedArgs, onSuccess, onFailure);
		}	
	}
	
	this.setChangedXmlMethod = function(methodName, argName, fixedArgs) {
		me._saveFunc = function(onSuccess, onFailure) {
			dh.formtable._doChange(controlId, true, methodName, argName, me._input.getValue(), fixedArgs, onSuccess, onFailure);
		}
	}
	
	this.setDescription("");
}