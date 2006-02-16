<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<%-- not used, client specifies title --%>
	<title></title>
	<dht:stylesheets/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util")
		var dhFlickrCreateAccount = function () {
			dojo.debug("invoking HaveFlickrAccount with true");
			window.external.application.HaveFlickrAccount(false)
		}
		var dhFlickrContinue = function () {
			dojo.debug("invoking HaveFlickrAccount with false");		
			window.external.application.HaveFlickrAccount(true)
		}
		
		var dhFlickrAwaitingAccount = function () {
			var mainDiv = document.getElementById("dhFlickrQuestion")
			dh.util.clearNode(mainDiv)
			var h4 = document.createElement("h4")
			mainDiv.appendChild(h4)
			h4.appendChild(document.createTextNode("Waiting for you to create Yahoo! account"))
			mainDiv.appendChild(document.createTextNode("Close the browser window when you are done."))
		}		
	</script>
</head>
<body>
	<dht:header>
		First Time Photo Setup
	</dht:header>

	<div id="dhMain">
		<div id="dhFlickrQuestion">
		<h2 id="dhFlickrHeader">Do you have a Yahoo! account?</h2>
		<div>
		DumbHippo can upload your photos to Yahoo! Flickr and share them.
		To get started, you need a Yahoo! account.
		</div>
		<center>
		<form>
			<input type="button" value="Create a new Yahoo! account" onclick="dhFlickrCreateAccount();"/>
			<br/>
			<input type="button" value="I already have a Yahoo! account" onclick="dhFlickrContinue();"/>
		</form>
		</center>
		</div>
	</div>

</body>
</html>
