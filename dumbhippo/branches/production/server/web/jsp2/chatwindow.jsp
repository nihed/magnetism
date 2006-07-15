<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="chatwindow" class="com.dumbhippo.web.pages.ChatWindowPage" scope="request"/>
<%-- only one of these params is expected at a time... chatId just means 
	"figure out whether it's post or group" which is less efficient but 
	some calling contexts might not know the type of chat --%>
<jsp:setProperty name="chatwindow" property="postId" param="postId"/>
<jsp:setProperty name="chatwindow" property="groupId" param="groupId"/>
<jsp:setProperty name="chatwindow" property="chatId" param="chatId"/>

<c:if test="${! chatwindow.aboutSomething}">
	<%-- no post or group, or invalid/not-allowed post or group --%>
	<dht:errorPage>Can't find this chat</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${chatwindow.title}"/></title>
   <link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/chatwindow.css"/>
	<dht:scriptIncludes/>
   <script type="text/javascript">
   	dojo.require("dh.chatwindow");
   	dh.chatwindow.setSelfId("${chatwindow.signin.userId}")
	</script>
	<dht:chatControl userId="${chatwindow.signin.userId}" chatId="${chatwindow.chatId}"/>
	<script for="dhChatControl" type="text/javascript" event="OnUserJoin(userId, photoUrl, name, participant)">
		dh.chatwindow.onUserJoin(userId, photoUrl, name, participant)
	</script>
	<script for="dhChatControl" language="javascript" event="OnUserLeave(userId)">
		dh.chatwindow.onUserLeave(userId)
	</script>
	<script for="dhChatControl" language="javascript" event="OnMessage(userId, photoUrl, name, text, timestamp, serial)">
		dh.chatwindow.onMessage(userId, photoUrl, name, text, timestamp, serial)
	</script>
	<script for="dhChatControl" language="javascript" event="OnReconnect()">
		dh.chatwindow.onReconnect()
	</script>
	<script for="dhChatControl" type="text/javascript" event="OnUserMusicChange(userId, arrangementName, artist, musicPlaying)">
		dh.chatwindow.onUserMusicChange(userId, arrangementName, artist, musicPlaying)
	</script>
	<script type="text/javascript">
		var chatControl = document.getElementById("dhChatControl")
       if (chatControl && chatControl.readyState && chatControl.readyState == 4) {
			chatControl.Join(true)
		}
	</script>
	<script defer type="text/javascript">
		dh.chatwindow.init()
	</script>
</head>
<body scroll="no" onload="dh.chatwindow.rescan()">
   <div id="dhChatPostInfoDiv">
   	${chatwindow.titleAsHtml}
   	<c:if test="${chatwindow.aboutPost}">
   	(from <dh:entity value="${chatwindow.post.poster}" photo="false"/>)
   	</c:if>
	</div>
	<div id="dhChatPeopleContainer">
       <div id="dhChatPeopleDiv"></div>
	    <div id="dhChatPeopleNW"></div>
	    <div id="dhChatPeopleSW"></div>
	</div> <!-- dhChatPeopleContainer -->
	<div id="dhChatMessagesContainer">	
       <div id="dhChatMessagesDiv"></div>    
       <div id="dhChatMessagesNE"></div>
       <textarea id="dhChatMessageInput" cols="40" rows="4"></textarea>
       <div id="dhChatSendButtonArea"></div>
       <img id="dhChatSendButton" src="/images2/${buildStamp}/chatSendButton.gif" onclick="dh.chatwindow.sendClicked()"></input>    
   </div>    
</body>
</html>
