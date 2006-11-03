<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.EntityView" %>
<%@ attribute name="onlineIcon" required="false" type="java.lang.Boolean" %>
<%@ attribute name="imageOnly" required="false" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${!empty who.homeUrl}">
	   	<jsp:element name="a">
	 		<jsp:attribute name="class">dh-underlined-link</jsp:attribute>
	 		<jsp:attribute name="href"><c:out value="${who.homeUrl}"></c:out></jsp:attribute>
	 		<jsp:body><dht3:entityLinkContent who="${who}" imageOnly="${imageOnly}"/></jsp:body>
	   	</jsp:element>
	</c:when>
	<c:otherwise>
		<dht3:entityLinkContent who="${who}" imageOnly="${imageOnly}"/>
	</c:otherwise>
</c:choose>
<c:if test="${dh:myInstanceOf(who, 'com.dumbhippo.server.views.PersonView') && onlineIcon}"> <dht3:presenceIcon who="${who}"/></c:if>
