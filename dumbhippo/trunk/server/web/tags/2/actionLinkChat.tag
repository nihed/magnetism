<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="chatId" required="true" type="java.lang.String" %>
<%-- must be "group" "post" "unknown" --%>
<%@ attribute name="kind" required="true" type="java.lang.String" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>

<c:choose>
   <%-- The browser.ie check is necessary because the dynamic hiding of
        the control when the chat object fails to load doesn't work
        correctly in firefox 1.0. could also be !(browser.gecko && !browser.gecko15)
        or something probably, but only IE is known to work anyhow --%>

	<c:when test="${signin.valid && browser.ie}">
		<c:set scope="page" var="joinChatUri" value="javascript:dh.actions.requestJoinRoom('${signin.userId}','${chatId}')"/>
	</c:when>
	<c:when test="${signin.valid && browser.linux && browser.gecko}">
		<c:set scope="page" var="joinChatUri" value="mugshot://${signin.server}/joinChat?id=${chatId}&kind=${kind}"/>
	</c:when>
	<c:otherwise>
		<%-- we don't know how to chat...  --%>
	</c:otherwise>
</c:choose>

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
	<dht:actionLink oneLine="${oneLine}" href="${joinChatUri}"
		title="${joinChatTitle}">Join Chat</dht:actionLink>
</c:if>
