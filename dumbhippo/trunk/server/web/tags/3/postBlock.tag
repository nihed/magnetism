<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.PostBlockView" %>

<div class="dh-stacker-block-content-post">
	<div class="dh-stacker-block-content-main">	
	<span class="dh-stacker-block-content-post-chatting"><c:out value="${block.postView.livePost.chattingUserCount}"/></span> people chatting <dht:actionLinkChat prefix=" | " oneLine="true" chatId="${block.postView.post.id}" kind="post"/>
	<c:forEach items="${block.recentMessages}" end="3" var="msg" varStatus="msgIdx">
		<div class="dh-stacker-block-chat-container">
		<img src="/images3/${buildStamp}/comment_iconchat_icon.png"/>
		<span class="dh-stacker-block-chat">
			<span class="dh-stacker-block-chat-message"><c:out value="${msg.msg.messageText}"/></span> -
			<span class="dh-stacker-block-chat-sender"><dht3:personLink who="${msg.senderView}"/></span>
		</span>
		</div>
	</c:forEach>
	</div>
</div>
