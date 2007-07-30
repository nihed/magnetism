<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht2" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<c:choose>
	<c:when test="${!empty errorText}">
		<c:set var="text" value='${errorText}'/>
	</c:when>
	<c:when test='${!empty param["text"]}'>
		<c:set var="text" value='${param["text"]}'/>
	</c:when>
	<c:otherwise>
		<c:set var="text" value="Something went wrong."/>
	</c:otherwise>
</c:choose>

<head>
	<title>Something went wrong.</title>
	<gnome:faviconIncludes/>
	<gnome:stylesheet name="site"/>
	<gnome:stylesheet name="error"/>
</head>
<body>
	<gnome:header/>
	<p class="gnome-error-header">
		<c:choose>
			<c:when test="${!empty errorHtml}">
				<c:out value="${errorHtml}" escapeXml="false"/>
			</c:when>
			<c:otherwise>
				<c:out value="${text}"/>
			</c:otherwise>
		</c:choose>
	</p>
	<c:choose>
		<c:when test="${!empty suggestionHtml}">
			<c:out value="${suggestionHtml}" escapeXml="false"/>
		</c:when>
		<c:otherwise>
			<p>
				<dht2:backLink/>
				<c:if test='${!empty param["retry"]}'> or <a href='${param["retry"]}'>try again</a>
				</c:if>
			</p>
		</c:otherwise>
	</c:choose>
</body>
</html>
