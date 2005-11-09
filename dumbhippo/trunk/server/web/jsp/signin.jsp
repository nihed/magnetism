<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Sign In</title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
	<dht:scriptIncludes/>
	<object classid="clsid:5A96BF90-0D8A-4200-A23B-1C8DABC0CC04" id="dhEmbedObject"></object>
	<script type="text/javascript">
	dojo.require("dh.login");
	dojo.require("dh.util");
	dojo.require("dojo.event.*");
	
	// We *are* the login dialog, we don't an internal popup
	dh.login.useDialog = false;
	
	function dhDoSignIn() {
	    dh.login.requireLogin(function() {
	    	// default is home since if you just type in the url that is useful
	    	// the windows client would specify next=close
	    	dh.util.goToNextPage("home");
	    });
	}
	
    dojo.event.connect(dojo, "loaded", dj_global, "dhDoSignIn");
	</script>
</head>
<body>
	<!--  I did have c:if test="${param.dialog == false}" on the 
		header but it looks fine even in the dialog  -->
	<dht:header>
		Sign In
	</dht:header>
	<!--  no toolbar, it's weird on this page -->
	
	<div id="dhMain">
		<form onsubmit="return false">
		<table class="dhSignInAlignment"><tr><td>
		<table class="dhSignInTable" align="center">
			<tr>
				<td>Email:</td>
				<td><input id="dhLoginDialogEmail" type="text" /></td>
			</tr>
			<tr>	
				<td colspan="2" align="right">
				<p id="dhLoginStatus"></p>
				</td>
			</tr>
			<tr>
				<td colspan="2" align="right">
				<input type="button" id="dhLoginDialogButton" value="Sign In">
				</td>
			</tr>
		</table>
		</td></tr></table>
		</form>
		<div id="dojoDebug"> <!-- where to put dojo debug spew --> </div>
	</div>
</body>
</html>
