<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>New Photo</title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
</head>
<body>
	<dht:header>
		New Photo
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<p>Your new photo looks like this:</p>
		<img src="/files/${photoLocation}/${photoFilename}"/>
		<p>(If this is your old photo, your computer didn't know to load the new one. <c:out value="${homePageLink}" escapeXml="false"/> and then press reload in your browser.)
		</p>

		<br/>
		<p><c:out value="${homePageLink}" escapeXml="false"/></p>

		<br/>
		<p>If you hate this photo, you can try again:</p>
		<dht:uploadPhoto location="${photoLocation}"/>
	</div>
</body>
</html>
