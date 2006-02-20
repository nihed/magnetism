<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
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
	<object classid="clsid:5A96BF90-0D8A-4200-A23B-1C8DABC0CC04" id="dhEmbedObject"></object>
	<object classid="clsid:2D40665F-8139-4cb5-BA39-A6E25A147F5D" id="dhChatControl">
		<param name="UserID" value="${framer.signin.userId}"/>
		<param name="PostID" value="${framer.post.post.id}"/>
	</object>
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
		var chatControl = document.getElementById("dhChatControl")
        if (chatControl && chatControl.readyState && chatControl.readyState == 4) {
			chatControl.Join(false)
		}
	</script>
</head>
<body onload="dh.framer.init()">

  <div>
	<table>
	<td>
	<div class="dh-share-shadow">
      <div class="dh-share">
	    <div class="dh-share-from">
	  	  <dh:entity value="${framer.post.poster}" photo="true"/>
	    </div>
	    <div class="dh-share-text">
	  	  <c:out value="${framer.post.titleAsHtml}" escapeXml="false"/>
		  <dh:favicon link="${framer.post.url}"/>
		  <div class="dh-share-description"><c:out value="${framer.post.textAsHtml}" escapeXml="false"/></div>
	    </div>
	    <div class="dh-share-to">Shared with <dh:entityList value="${framer.post.recipients}" skipRecipientId="${hideRecipientId}"/> by <dh:entity value="${framer.post.poster}" photo="false"/>&nbsp;<dh:presence value="${framer.post.poster}"/></div>    
      </div> <!-- share -->
    </div> <!-- share-shadow -->
	</td>
	<td class="action-area">
	   <table class="action-area" cellspacing="2px">
	   <c:if test="${!param.browserBar}">
		   <tr>
		       <td class="action" nowrap><a class="action action-box" href="${url}" target=_top>X</a></td>
		       <td class="action" nowrap><a class="action" href="${url}" target=_top>Remove Frame</a></td>
		   </tr>
	   </c:if>
	   <tr>
	       <td class="action" nowrap><a class="action action-box" href="javascript:dh.framer.goHome()" target=_top>&#171;</a></td>	
	       <td class="action" nowrap><a class="action" href="javascript:dh.framer.goHome()" target=_top>Back Home</a></td>       
	   </tr>
	   <tr>
	       <td class="action" nowrap><a class="action action-box highlight-action" href="javascript:dh.framer.openForwardWindow();">&#187</a></td>
	       <td class="action" nowrap><a class="action highlight-action" href="javascript:dh.framer.openForwardWindow();">Forward To Others</a></td>
	   </tr>
	   <c:if test="${framer.signin.userId != null}">
	       <tr id="dhChatLinkRow">
		       <td class="action" nowrap><a class="action highlight-action" href='javascript:dh.actions.requestJoinRoom("${framer.signin.userId}","${framer.post.post.id}")'><dh:png klass="dh-chat-icon" src="/images/${buildStamp}/chat.png" style="width: 16; height: 16;"/></a></td>
	    	   <td class="action" nowrap><a class="action highlight-action" href='javascript:dh.actions.requestJoinRoom("${framer.signin.userId}","${framer.post.post.id}")'>Chat About This</a></td>
		   </tr>
	   </c:if>
	   <tr id="dhChatPreviewRow">
	       <td id="dhChatPreview" nowrap colspan=2>
			    <div>Just looking at the page: <span id="dhChatVisitorList"></span></div>
			    <div>Currently chatting: <span id="dhChatParticipantList"></span></div>
	       </td>
	   </tr>
	   </table>
	</td>
	</tr>
	</table>
  </div>

</body>
</html>
