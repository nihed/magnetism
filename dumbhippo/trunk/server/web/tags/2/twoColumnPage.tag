<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="alwaysShowSidebar" required="false" type="java.lang.Boolean" %>
<%@ attribute name="neverShowSidebar" required="false" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${alwaysShowSidebar}">
		<c:set var="showSidebar" value="true" scope="request"/>
	</c:when>
	<c:when test="${neverShowSidebar}">
		<c:set var="showSidebar" value="false" scope="request"/>
	</c:when>
	<c:otherwise>
		<c:set var="showSidebar" value="${signin.valid}" scope="request"/>
	</c:otherwise>
</c:choose>

<dht:body>
	<dht:header/>
		<div id="dhPageContent">
			<jsp:doBody/>
		</div>
	<dht:footer/>
</dht:body>
