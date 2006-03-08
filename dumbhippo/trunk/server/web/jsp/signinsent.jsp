<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Sign-in Link Sent</title>
	<dht:stylesheets />
</head>
<dht:bodyWithAds>
		<dht:header>
			Link Sent
		</dht:header>
		<dht:toolbar/>
		<div id="dhMainArea">
	
			<h3>
				Sign-in link sent to <c:out value="${address}"/>; click on that link to sign in.
			</h3>
			
			<p><a href="/home">Home</a></p>
			<p><a href="/main">Main</a></p>
		</div>
</dht:bodyWithAds>
</html>
