<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="framer" class="com.dumbhippo.web.pages.FramerPage" scope="request"/>
<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="postId"/>

<dht3:validateFramer page="framer" framer="${framer}"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>
<c:set var="url" value="${framer.post.url}" scope="page"/>
<c:set var="description" value="${framer.post.post.text}" scope="page"/>

<dht:setJoinChatUri chatId="${framer.post.post.id}"/>

<c:set var="lastMessageId" value='0'/>
<c:if test='${framer.block.lastMessage != null}'>
    <c:set var="lastMessageId" value='${framer.block.lastMessage.msg.id}'/> 
</c:if>

<head>
   <title><c:out value="${title}"/> - Mugshot</title>
   	<dht3:stylesheet name="site" iefixes="true"/>	
   	<dht3:stylesheet name="chatwindow" iefixes="true"/>	
	<dht3:stylesheet name="framer" iefixes="true" lffixes="true"/>			
	<dht:faviconIncludes/>
    <dh:script modules="dh.actions,dh.framer"/>
	<script type="text/javascript">
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
		dh.framer.initialLastMessageId = "${lastMessageId}";
		dh.framer.initialMessageCount = "${framer.block.messageCount}";
		dh.framer.currentMessageCount = "${framer.block.messageCount}";
	</script>
</head>	

<body onload="dh.framer.init()">
    <table id="dhFramerTable" cellpadding="0" cellspacing="5">
        <tr>
            <td id="dhFramerLeft" width="50%" valign="top">
				<dht3:framerPost post="${framer.post}" block="${framer.block}"/>
				<div id="dhPostSwarmInfo" class="dh-block-details">
					Who's around: <span id="dhPostViewingListPeople" class="dh-entity-list"></span>
				</div>
            </td>
            <td width="50%" valign="top">
                <div id="dhPostChatLog">
                    <div id="dhQuipper">
                        Chat preview:
                    </div>
                    <c:if test="${!param.browserBar}"> 
                        <div id="dhFramerClose">
                            <a href="javascript:dh.framer.removeFrame();"><img src="/images3/${buildStamp}/x-close.png"/></a>
                        </div>    
                    </c:if>
				    <div id="dhPostChatMessages">
					    <!-- dynamically gets divs class="dh-chat-message" -->
					</div>
				</div>	
				<div id="dhFrameActions">
			        <c:if test="${!empty joinChatUri}">
				        <a id="dhPostJoinChat" href="${joinChatUri}">All quips (<span id="dhQuipsCount">${framer.block.messageCount}</span>)</a> |
				    </c:if>
				    <a href="javascript:dh.framer.openForwardWindow();">Share this</a> |
                    <a href="javascript:dh.framer.goHome();">Mugshot home</a>
                </div>     	
            </td>
        </tr>
    </table>        
</body>
</html>