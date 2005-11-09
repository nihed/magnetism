<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Invite Sent</title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
	<dht:header>
		Invite Sent
	</dht:header>
	<dht:toolbar/>
	<div id="dhMain">

		<h3>
			Congratulations, the invitation to 
		    <c:out value="${fullName}"/>
		    (<c:out value="${email}"/>) was sent
		</h3>
		<!--  print the link now -->
		<c:url value="verify?authKey=${authKey}" var="authurl"/>
		<p>Invite url: <a href="${authurl}">${authurl}</a></p>

		<p><a href="/invite">Invite someone else</a></p>
		<p><a href="/home">Home</a></p>
		<p><a href="/main">Main</a></p>
	</div>
</body>
</html>
