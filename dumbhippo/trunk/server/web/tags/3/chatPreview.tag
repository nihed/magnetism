<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.blocks.BlockView" %>
<%@ attribute name="chatId" required="true" type="java.lang.String" %>
<%@ attribute name="chatKind" required="true" type="java.lang.String" %>
<%@ attribute name="chattingCount" required="true" type="java.lang.Integer" %>

<span class="dh-stacker-block-content-post-chatting"><c:out value="${chattingCount}"/></span> people chatting <dht:actionLinkChat prefix=" | " oneLine="true" chatId="${chatId}" kind="${chatKind}"/>
<c:forEach items="${block.recentMessages}" end="3" var="msg" varStatus="msgIdx">
	<div class="dh-stacker-block-chat-container">
	<img src="/images3/${buildStamp}/comment_iconchat_icon.png"/>
	<span class="dh-stacker-block-chat">
		<span class="dh-stacker-block-chat-message"><c:out value="${msg.msg.messageText}"/></span> -
		<span class="dh-stacker-block-chat-sender"><dht3:entityLink who="${msg.senderView}"/></span>
	</span>
	</div>
</c:forEach>