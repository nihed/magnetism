<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showChatLink" required="false" type="java.lang.Boolean" %>

<dh:default var="showChatLink" value="true"/>

<div id="dhStackerBlockChat-${blockId}">
	<c:forEach items="${block.recentMessages}" end="4" var="msg">
		<dht3:chatMessage msg="${msg}"/>
	</c:forEach>
	<c:if test="${showChatLink && !empty block.chatId}">
		<dht:setJoinChatUri chatId="${block.chatId}"/>
		<dht:actionLink title="See all Quips and Comments" href="${joinChatUri}">Quips and Comments (${block.messageCount}) &raquo;</dht:actionLink>
	</c:if>
</div>