dojo.provide("dh.login");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.Dialog");
dojo.require("dh.server");
dojo.require("dh.util");

dh.login.dialogContentHtml =
  '<div dojoType="dialog" id="dhLoginDialog" class="dhInvisible">'
+ '	<form onsubmit="return false" class="dhDialogContent">'
+ '		<table>'
+ '			<tr>'
+ '				<td>Email:</td>'
+ '				<td><input id="dhLoginDialogEmail" type="text"/></td>'
+ '			</tr>'
+ '			<tr>'
+ '				<td colspan="2" align="right"><p id="dhLoginStatus" class="dhStatusLabel"></p></td>'
+ '			</tr>'
+ '			<tr>'
+ '				<td colspan="2" align="right">'
+ '				<input type="button" id="dhLoginDialogButton" value="Sign In">'
+ '				</td>'
+ '			</tr>'
+ '		</table>'
+ '	</form>'
+ '</div>';


dh.login.useDialog = true;
dh.login.loggedInUserId = null;
dh.login.postLoginQueue = [];

dh.login.NOT_LOGGED_IN = 0;
dh.login.CHECK_LOGIN_IN_FLIGHT = 1;
dh.login.SHOWING_DIALOG = 2;
dh.login.SUBMIT_LOGIN_IN_FLIGHT = 3;
dh.login.LOGGED_IN = 4;

dh.login.loginState = dh.login.NOT_LOGGED_IN;

dh.login.dialog = null;

dh.login.parsePersonId = function(str) {
	// not very rigorous but good enough
	str = str.replace(/^\s+/g, "");
	str = str.replace(/\s+$/g, "");
	if (str.match(/[a-f0-9]+/))
		return str;
	else
		return null;
}

dh.login.doNowLoggedIn = function(personId) {
	dh.login.loggedInUserId = personId;
	dh.login.loginState = dh.login.LOGGED_IN;
	if (dh.login.dialog) {
		dh.login.dialog.hide();
	}
	var q = dh.login.postLoginQueue;
	dh.login.postLoginQueue = [];
	for (var i in q) {
		q[i]();
	}
}

dh.login.displayStatus = function(message, error) {
	dojo.debug("showing status " + message);
	var node = document.getElementById('dhLoginStatus');
	if (error) {
		dojo.html.addClass(node, "dhValidityError");
	} else {
		dojo.html.removeClass(node, "dhValidityError");
	}
	dojo.dom.textContent(node, message);
}

dh.login.handleLoadLogin = function(type, data, http) {
	dj_debug("checklogin/dologin got back data " + dhAllPropsAsString(data));
	
	var wasSubmitLogin = false;
	
	// on success, doNowLoggedIn will overwrite NOT_LOGGED_IN
	if (dh.login.loginState == dh.login.CHECK_LOGIN_IN_FLIGHT) {
		dojo.debug("--state was CHECK_LOGIN_IN_FLIGHT");
		dh.login.loginState = dh.login.NOT_LOGGED_IN;
	} else if (dh.login.loginState == dh.login.SUBMIT_LOGIN_IN_FLIGHT) {
		dojo.debug("--state was SUBMIT_LOGIN_IN_FLIGHT");
		wasSubmitLogin = true;
		dh.login.loginState = dh.login.NOT_LOGGED_IN;
	} else if (dh.login.loginState == dh.login.LOGGED_IN) {
		dojo.debug("??? already logged in? state = " + dh.login.loginState);
		return; // should not happen really
	}

	var asPersonId = null;
	if (data != "false") {
	  	asPersonId = dh.login.parsePersonId(data);
	  	dojo.debug("asPersonId = " + asPersonId);
	}
	
	if (asPersonId) {
		dojo.debug("person ID looks good - we are logged in as " + asPersonId);
		dh.login.doNowLoggedIn(asPersonId);
	} else {
		if (wasSubmitLogin) {
			dh.login.displayStatus("Login unsuccessful", true);
		}
		dh.login.showDialog();
	}
}

dh.login.handleErrorLogin = function(type, error, http) {
	dojo.debug("checklogin/dologin got back an error " + dhAllPropsAsString(error));
	dh.login.displayStatus(http.statusText, true);
	dh.login.showDialog();
}

dh.login.submitLogin = function() {
	if (dh.login.loginState == dh.login.NOT_LOGGED_IN ||
		dh.login.loginState == dh.login.SHOWING_DIALOG) {
				
		if (dojo.string.isBlank(dh.login.emailEntry.value)) {
			dh.login.displayStatus("Please give an email address", true);
			return;
		}
		
		dojo.debug("submitting login form");
		
		dh.login.displayStatus("Please wait...", false);
		
		dh.login.loginState = dh.login.SUBMIT_LOGIN_IN_FLIGHT;
		
		dh.server.getTextPOST("dologin",
		  				  	  { "email" : dh.login.emailEntry.value },
						  	  dh.login.handleLoadLogin,
						  	  dh.login.handleErrorLogin);
	}
}
dhLoginSubmitLogin = dh.login.submitLogin; // so we can connect it

dh.login.showDialog = function() {

	// note that dojo.widget.Dialog.show() is NOT a no-op if already showing

	if (dh.login.loginState == dh.login.SHOWING_DIALOG) {
		dojo.debug("already showing dialog");
		return;
	}

	dojo.debug("showing login dialog");

 	// clear the status display
	dh.login.displayStatus("", false);

	dh.login.loginState = dh.login.SHOWING_DIALOG;

	if (dh.login.useDialog) {
		dh.login.createDialog();
	
		dh.login.dialog.show();
	} else {
		dh.login.connectForm();
	}
}

dh.login.requireLogin = function(doAfterLoginFunc) {

	if (dh.login.loginState == dh.login.LOGGED_IN) {
		dojo.debug("already logged in as " + dh.login.loggedInUserId);
		doAfterLoginFunc();
		return;
	}

	dojo.debug("adding " + doAfterLoginFunc + " to queue for post-login execution");
	dh.login.postLoginQueue.push(doAfterLoginFunc);

	// only start the process if it isn't already in process
	if (dh.login.loginState == dh.login.NOT_LOGGED_IN) {
	
		dojo.debug("checking login state");
	
		dh.login.loginState = dh.login.CHECK_LOGIN_IN_FLIGHT;
		dh.server.getTextPOST("checklogin",
		  				  	  {  },
						  	  dh.login.handleLoadLogin,
						  	  dh.login.handleErrorLogin);
	}
}

dh.login.onKeyPress = function(event) {
	//dojo.debug("onKeyPress " + dhAllPropsAsString(event));
	if (event.keyCode == 13) {
		dh.login.submitLogin();
	} 
}
dhLoginOnKeyPress = dh.login.onKeyPress; // so it can be event handler

dh.login.createDialog = function() {

	if (dh.login.dialog != null)
		return;

	dojo.debug("creating login dialog");

	var nodes = dojo.html.createNodesFromText(dh.login.dialogContentHtml);
	var node = nodes[0];
	// dojo gets confused with the unparented node, but this causes flicker. we'll 
	// do a "hide all nodes at first" hack later.
	document.body.appendChild(node);
	
	//dojo.debug("created nodes " + dhAllPropsAsString(node));

	dh.login.dialog = dojo.widget.fromScript("dialog", 
											{}, // props,
											node);

	dh.login.dialog.setBackgroundColor("#ccc");
	
	// the transparency thing is crazy slow on Linux prior to ff 1.5
	if (dh.util.disableOpacityEffects) {
		dh.login.dialog.effect = "";
		dh.login.dialog.setBackgroundOpacity(1.0);
	}
	
	dh.login.connectForm();
	
	// now display our content nodes
	dh.util.show(node);
}

dh.login.connectForm = function() {
	dh.login.emailEntry = document.getElementById("dhLoginDialogEmail");
	
	var btn = document.getElementById("dhLoginDialogButton");
	dojo.event.connect(btn, "onclick", dj_global, "dhLoginSubmitLogin");
	dojo.event.connect(dh.login.emailEntry, "onkeypress", dj_global, "dhLoginOnKeyPress");
}