<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="postId"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>
<c:set var="url" value="${framer.post.url}" scope="page"/>
<c:set var="description" value="${framer.post.post.text}" scope="page"/>

<head>
	<title><c:out value="${title}"/></title>
	<dht:stylesheets href="frames.css" iehref="frames-iefixes.css" />
	<dht:scriptIncludes/>
    <script type="text/javascript">
    	dojo.require("dh.framer")
    	dh.framer.setSelfId("${framer.signin.userId}")
    	dh.framer.forwardUrl = <dh:jsString value="${url}"/>;
		dh.framer.forwardTitle = <dh:jsString value="${title}"/>;
    	dh.framer.openForwardWindow = function() {
    		dh.util.openShareLinkWindow(dh.framer.forwardUrl, dh.framer.forwardTitle);
    	}
	</script>
	<dht:embedObject/>
	<dht:chatControl userId="${framer.signin.userId}" chatId="${framer.post.post.id}"/>
	<script for="dhChatControl" type="text/javascript" event="OnUserJoin(userId, version, name, participant)">
		dh.framer.onUserJoin(userId, version, name, participant)
	</script>
	<script for="dhChatControl" language="javascript" event="OnUserLeave(userId)">
		dh.framer.onUserLeave(userId)
	</script>
	<script for="dhChatControl" language="javascript" event="OnMessage(userId, version, name, text, timestamp, serial)">
		dh.framer.onMessage(userId, version, name, text, timestamp, serial)
	</script>
	<script for="dhChatControl" language="javascript" event="OnReconnect()">
		dh.framer.onReconnect()
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
			var chatControl = document.getElementById("dhChatControl")
			chatControl.Leave(false)
			dhClosed = true
		}
		
		var chatControl = document.getElementById("dhChatControl");
        if (!dhClosed && chatControl && chatControl.readyState && chatControl.readyState == 4) {
			chatControl.Join(false);
		}
	</script>
</head>
<body onload="dh.framer.init()">
	<div id="dhMain">

		<!-- under everything -->
		<div id="dhTopShadow">
		</div>
		
		<!--  first in page so it's on the bottom -->
		<div id="dhPostChatAreaContainer">
			<div id="dhPostChatArea">
			
				<div id="dhChatAreaNE"></div>
				<div id="dhChatAreaBorderWhiteout"></div>
			
				<div id="dhPostChatLabel">Chat <span id="dhPostChatCount"></span></div><!-- dhPostChatLabel -->
				
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
					   <%-- The browser.gecko check is here because the dynamic hiding of
					        the control when the chat object fails to load doesn't work
					        correctly in firefox 1.0 --%>
					   <c:if test="${framer.signin.userId != null && !browser.gecko}">
							<div class="dh-post-action" id="dhPostJoinChat">
								<a href="javascript:dh.actions.requestJoinRoom('${framer.signin.userId}','${framer.post.post.id}')"><img class="dh-post-action-arrow" src="/images/framerArrowRight.gif"/></a><a href="javascript:dh.actions.requestJoinRoom('${framer.signin.userId}','${framer.post.post.id}')">Join Chat</a>
							</div>
						</c:if>
					   <c:if test="${!param.browserBar}">
							<div class="dh-post-action">
								<a href="${url}" target="_top"><img class="dh-post-action-arrow" src="/images/framerArrowRight.gif"/></a><a href="${url}" target="_top">Remove Frame</a>
							</div>
						</c:if>
						<div class="dh-post-action">
							<a href="javascript:dh.framer.goHome();" target="_top"><img class="dh-post-action-arrow" src="/images/framerArrowRight.gif"/></a><a href="javascript:dh.framer.goHome()" target="_top">Back Home</a>
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
			<!-- in Z-order -->
			<div id="dhPostBubbleBackground"></div>
			<div id="dhPostBubbleSE"></div>
			<div id="dhPostBubbleNE"></div>
			<div id="dhPostBubbleNW"></div>
			<div id="dhPostBubbleSW"></div>
			<div id="dhPostBubble">
				<dht:postBubble noBorder="true" post="${framer.post}" hideRecipientId="${framer.signin.userId}"/>
			</div><!-- dhPostBubble -->
		</div>
		
		<div id="dhPostViewingList">
			<div id="dhPostViewingListLabel">Who's viewing this page:</div>
			<div id="dhPostViewingListPeople"></div>
		</div><!-- dhPostViewingList -->
	
	</div>
</body>
</html>
