<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.pages.FramerPage" scope="request"/>
<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="postId"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>
<c:set var="url" value="${framer.post.url}" scope="page"/>
<c:set var="description" value="${framer.post.post.text}" scope="page"/>

<%-- FIXME this chat stuff is cut-and-pasted from actionLinkChat in 
     the new tags/2 tags; need to unify ... --%>
<c:choose>
   <%-- The browser.ie check is necessary because the dynamic hiding of
        the control when the chat object fails to load doesn't work
        correctly in firefox 1.0. could also be !(browser.gecko && !browser.gecko15)
        or something probably, but only IE is known to work anyhow --%>

	<c:when test="${signin.valid && browser.ie}">
		<c:set scope="page" var="joinChatUri" value="javascript:dh.control.control.showChatWindow('${framer.post.post.id}')"/>
	</c:when>
	<c:when test="${signin.valid && browser.linux && browser.gecko}">
		<c:set scope="page" var="joinChatUri" value="mugshot://${signin.server}/joinChat?id=${framer.post.post.id}&kind=post"/>
	</c:when>
	<c:otherwise>
		<%-- we don't know how to chat...  --%>
	</c:otherwise>
</c:choose>

<head>
	<title><c:out value="${title}"/></title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/framer.css">	
	<!--[if IE]>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/framer-iefixes.css">
	<![endif]-->
	<dht:scriptIncludes/>
    <script type="text/javascript">
    	dojo.require("dh.framer");
    	dh.framer.setSelfId("${framer.signin.userId}")
    	dh.framer.chatId = "${framer.post.post.id}"
    	dh.framer.forwardUrl = <dh:jsString value="${url}"/>;
		dh.framer.forwardTitle = <dh:jsString value="${title}"/>;
	</script>
	<script type="text/javascript">
		// This is called by the Explorer browser bar code when the browser bar
		// is closed. The web page is kept around in that case, so we need to leave
		// the chatroom. We do this by deleting the chat control to reduce the chance
		// of resource leaks, and also avoid race conditions if we are closed
		// before we finish loading. (For unknown reasons, putting this in framer.js 
		// doesn't work.)
		var dhClosed = false
		function dhBarClosed() {
			dh.framer._chatRoom.leave()
			dhClosed = true
		}
	</script>
</head>
<body onload="dh.framer.init()">
	<div id="dhMain">

		<!-- under everything -->
		<div id="dhTopShadow">
		</div>
		
		<div id="dhPostActionsTitle"></div>
		
		<!--  first in page so it's on the bottom -->
		<div id="dhPostChatAreaContainer">
			<div id="dhPostChatArea">
			
				<div id="dhChatAreaNE"></div>
				<div id="dhChatAreaBorderWhiteout"></div>
				
				<div id="dhPostSwarmInfo">
					<span id="dhPostViewingListLabel">Who's swarming this page: </span>
					<span id="dhPostViewingListPeople"></span>
				</div>
				
				<div id="dhPostChatLogContainer">
					<div id="dhPostChatLog">
					
						<div id="dhPostChatNames">
							<!-- dynamically gets div class="dh-chat-name" -->
						</div>
						<div id="dhPostChatDivider"></div>
						<div id="dhPostChatMessages">
							<!-- dynamically gets div class="dh-chat-message" -->
						</div>
					
					</div><!-- dhPostChatLog -->
				</div><!-- dhPostChatLogContainer -->
	
				<div id="dhPostActionsContainer">
					<!-- in Z-order -->
					<div id="dhPostActionsSW"></div>
					<div id="dhPostActionsSE"></div>
					<div id="dhPostActionsBorderBlueout"></div>
					<div id="dhPostActions">
					   <c:if test="${!empty joinChatUri}">
							<div class="dh-post-action" id="dhPostJoinChat">
								<a href="${joinChatUri}"><img class="dh-post-action-arrow" src="/images/framerArrowRight.gif"/></a><a href="${joinChatUri}">Join Chat</a> <span id="dhPostChatCount"></span>
							</div>
					   </c:if>		
					   <c:if test="${!param.browserBar}">
							<div class="dh-post-action">
								<a href="${url}" target="_top"><img class="dh-post-action-arrow" src="/images/framerArrowRight.gif"/></a><a href="${url}" target="_top">Remove Frame</a>
							</div>
						</c:if>
						<div class="dh-post-action">
							<a href="javascript:dh.framer.goHome();"><img class="dh-post-action-arrow" src="/images/framerArrowRight.gif"/></a><a href="javascript:dh.framer.goHome()">Back Home</a>
						</div>
						<div class="dh-post-action">
							<a href="javascript:dh.framer.openForwardWindow();"><img class="dh-post-action-arrow" src="/images/framerArrowRight.gif"/></a><a href="javascript:dh.framer.openForwardWindow();">Forward to Others</a>
						</div>
					</div><!-- dhPostActions -->
				</div><!-- dhPostActionsContainer -->
	
			</div><!-- dhPostChatArea -->
		</div><!-- dhPostChatAreaContainer -->

		<!-- on top of stuff in the chat area, under other stuff -->
		<div id="dhBottomShadow">
		</div>
		
		<div id="dhPostBubbleContainer">	
			<div id="dhPostBubbleContainerBackground"></div>
			<div id="dhPostBubbleContainerBackgroundLeft"></div>			
			<div id="dhPostBubbleContainerBackgroundRight"></div>			
			<!-- in Z-order -->
			<div id="dhPostBubbleBackground"></div>
			<div id="dhPostBubbleSE"></div>
			<div id="dhPostBubbleNE"></div>
			<div id="dhPostBubbleNW"></div>
			<div id="dhPostBubbleSW"></div>
			<div id="dhPostBubble">
				<dht:framerPost post="${framer.post}"/>			
			</div><!-- dhPostBubble -->
		</div>
	</div>
</body>
</html>
