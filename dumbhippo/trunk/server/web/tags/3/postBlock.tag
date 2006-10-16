<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.PostBlockView" %>
<%@ attribute name="cssClass" required="true" type="java.lang.String" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<dht3:blockContainer cssClass="${cssClass}" blockId="${blockId}" expandable="true">
	<dht3:blockHeader icon="/images3/${buildStamp}/webswarm_icon.png" blockId="${blockId}">
		<dht3:blockHeaderLeft>
			<span class="dh-stacker-block-title-type">Web Swarm</span>:
			<span class="dh-stacker-block-title-title">
				<jsp:element name="a">
					<jsp:attribute name="class">dh-underlined-link</jsp:attribute>
					<jsp:attribute name="href"><c:out value="${block.webTitleLink}"/></jsp:attribute>
					<jsp:body><c:out value="${block.webTitle}"/></jsp:body>
				</jsp:element>		
			</span>
		</dht3:blockHeaderLeft>
		<dht3:blockHeaderRight blockId="${blockId}">
			<c:choose>
				<c:when test="${block.postView.livePost.totalViewerCount == 1}">1 view</c:when>
				<c:otherwise>${block.postView.livePost.totalViewerCount} views</c:otherwise>
			</c:choose>
			<c:if test="${signin.valid}">
				<c:choose>
					<c:when test="${block.postView.favorite}">
						<c:if test="${favesMode != 'add-only'}">
						 | <a href="javascript:dh.actions.setPostFavorite('${block.postView.post.id}', false);">Remove as Fave</a>
						</c:if>
					</c:when>
					<c:otherwise>
						| <a href="javascript:dh.actions.setPostFavorite('${block.postView.post.id}', true);">Add to Faves</a>
					</c:otherwise>
				</c:choose>
			  | <jsp:element name="a">
			  	  <jsp:attribute name="href">javascript:dh.util.openShareLinkWindow(<dh:jsString value="${block.postView.post.url}"/>, <dh:jsString value="${block.postView.post.title}"/>);</jsp:attribute>
			  	  <jsp:body>Web Swarm this</jsp:body>
			  	</jsp:element>
			</c:if>				
		</dht3:blockHeaderRight>
	</dht3:blockHeader>
	<dht3:blockDescription>
		${block.postView.textAsHtml}
	</dht3:blockDescription>
	<dht3:blockContent blockId="${blockId}">
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
	</dht3:blockContent>
</dht3:blockContainer>

