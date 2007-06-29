<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="chatId" required="true" type="java.lang.String" %>
<%-- must be "group" "post" "unknown" --%>
<%@ attribute name="kind" required="true" type="java.lang.String" %>
<%@ attribute name="prefix" required="false" type="java.lang.String" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="linkText" required="false" type="java.lang.String" %>

<c:if test="${empty linkText}">
	<c:set scope="page" var="linkText" value="Join Chat"/>
</c:if>
<dht:setJoinChatUri chatId="${chatId}"/>

<c:if test="${!empty joinChatUri}">
	<c:choose>
		<c:when test="${kind == 'group'}">
			<c:set scope="page" var="joinChatTitle" value="Chat with other group members"/>
		</c:when>
		<c:when test="${kind == 'post'}">
			<c:set scope="page" var="joinChatTitle" value="Chat about this page"/>
		</c:when>
		<c:otherwise>
			<c:set scope="page" var="joinChatTitle" value="Chat about this"/>
		</c:otherwise>
	</c:choose>
	<c:out value="${prefix}"/>
	<dht:actionLink oneLine="${oneLine}" href="${joinChatUri}"
		title="${joinChatTitle}"><c:out value="${linkText}"/></dht:actionLink>
</c:if>
