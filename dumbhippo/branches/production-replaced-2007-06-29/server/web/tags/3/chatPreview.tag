<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
<%@ attribute name="chatId" required="true" type="java.lang.String" %>
<%@ attribute name="chatKind" required="true" type="java.lang.String" %>
<%@ attribute name="chattingCount" required="true" type="java.lang.Integer" %>
<%@ attribute name="showChatLink" required="false" type="java.lang.Boolean" %>

<c:if test="${showChatLink}">
	<c:choose>
		<c:when test="${chattingCount > 0}">
			<span class="dh-stacker-block-content-post-chatting"><c:out value="${chattingCount}"/></span> people chatting <dht:actionLinkChat prefix=" | " oneLine="true" chatId="${chatId}" kind="${chatKind}"/>
		</c:when>
		<c:otherwise><dht:actionLinkChat oneLine="true" chatId="${chatId}" kind="${chatKind}"/></c:otherwise>
	</c:choose>
</c:if>
<c:forEach items="${block.recentMessages}" end="3" var="msg">
	<dht3:chatMessage msg="${msg}"/>
</c:forEach>