<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Invite Sent</title>
	<dht:stylesheets />
	<dht:scriptIncludes/>
</head>
<body>
<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

		<dht:toolbar/>

		<h3>
			Congratulations, the invitation to 
		    <c:out value="${email}"/> was sent.
		</h3>

		<c:if test="${!empty note}">
			<p>${note}</p>
		</c:if>

		<c:choose>
			<c:when test="${remaining > 0}">
				<p>You can invite ${remaining} more people. <a href="/invite">Invite someone else now.</a></p>
			</c:when>
			<c:otherwise>
				<p>You can't invite anyone else for now. But don't worry, 
					we'll expand the site soon.</p>
			</c:otherwise>
		</c:choose>
		
		<p><a href="/home">Home</a></p>
		<p><a href="/main">Main</a></p>
	</div>
</div>
</body>
</html>
