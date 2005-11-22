<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Error</title>
	<dht:stylesheets />
</head>
<body>
	<dht:header>
		Error
	</dht:header>

	<div id="dhMain">
		
		<h2>Oops!</h2>
		
		<c:choose>
			<c:when test="${!empty errorHtml}">
				<p><c:out value="${errorHtml}" escapeXml="false"/></p>
			</c:when>
			<c:otherwise>
				<p><c:out value="${errorText}"/></p>
			</c:otherwise>
		</c:choose>

		<c:choose>
			<c:when test="${!empty suggestionHtml}">
				<p><c:out value="${suggestionHtml}" escapeXml="false"/></p>
			</c:when>
			<c:otherwise>
				<p><a href="/home">Home</a></p>
			</c:otherwise>
		</c:choose>
		
	</div>
	
</body>
</html>
