<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Error</title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
</head>
<body>
	<dht:header>
		Error
	</dht:header>

	<div id="dhMain">
		
		<h2>Oops!</h2>
		
		<p><c:out value="${errorText}"/></p>
		
		<p><a href="home">Home</a></p>
		
	</div>
	
</body>
</html>
