<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="msg" required="true" type="com.dumbhippo.server.views.ChatMessageView" %>
<%@ attribute name="id" required="false" type="java.lang.String" %>

<div class="dh-stacker-block-chat-container" id="${id}">
	<c:choose>
		<c:when test="${dh:enumIs(msg.msg.sentiment, 'INDIFFERENT')}">
			<dh:png src="/images3/${buildStamp}/comment_iconchat_icon.png" style="width: 11px; height: 11px;; overflow: hidden;"/>
		</c:when>
		<c:when test="${dh:enumIs(msg.msg.sentiment, 'LOVE')}">
			<dh:png src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12px; height: 11px; overflow: hidden;"/>
		</c:when>
		<c:when test="${dh:enumIs(msg.msg.sentiment, 'HATE')}">
			<dh:png src="/images3/${buildStamp}/quiphate_icon.png" style="width: 11px; height: 11px; overflow: hidden;"/>
		</c:when>
	</c:choose>
	<span class="dh-stacker-block-chat">
		<span class="dh-stacker-block-chat-message"><c:out value="${msg.msg.messageText}"/></span> -
		<span class="dh-stacker-block-chat-sender"><dht3:entityLink who="${msg.senderView}"/></span>
		<span class="dh-stacker-block-time-ago"><c:out value="${msg.timeAgo}"/></span>
	</span>
</div>