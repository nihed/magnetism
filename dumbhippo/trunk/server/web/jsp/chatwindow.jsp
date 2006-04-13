<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="postId"/>

<head>
	<title><c:out value="${framer.post.title}"/></title>
	<dht:stylesheets href="chatwindow.css" iehref="chatwindow-iefixes.css" />
	<dht:scriptIncludes/>
    <script type="text/javascript">
    	dojo.require("dh.chatwindow");
    	dh.chatwindow.setSelfId("${framer.signin.userId}")
	</script>
	<object classid="clsid:2D40665F-8139-4cb5-BA39-A6E25A147F5D" id="dhChatControl">
		<param name="UserID" value="${framer.signin.userId}"/>
		<param name="PostID" value="${framer.post.post.id}"/>
	</object>
	<script for="dhChatControl" type="text/javascript" event="OnUserJoin(userId, version, name, participant)">
		dh.chatwindow.onUserJoin(userId, version, name, participant)
	</script>
	<script for="dhChatControl" language="javascript" event="OnUserLeave(userId)">
		dh.chatwindow.onUserLeave(userId)
	</script>
	<script for="dhChatControl" language="javascript" event="OnMessage(userId, version, name, text, timestamp, serial)">
		dh.chatwindow.onMessage(userId, version, name, text, timestamp, serial)
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
    	${framer.post.titleAsHtml} (from <dh:entity value="${framer.post.poster}" photo="false"/>)
	</div>
	<div id="dhChatPeopleContainer">
        <div id="dhChatPeopleDiv"></div>
        <div id="dhChatPeopleNE"></div>
	    <div id="dhChatPeopleNW"></div>
	</div> <!-- dhChatPeopleContainer -->
    <div id="dhChatAdsDiv">
        <div id="dhChatAdsSE"></div>
        <div id="dhChatAdsInnerDiv">
        	<dht:ad src="${psa1}"/>
        </div>
    </div>
    <div id="dhChatMessagesDiv"></div>
    <textarea id="dhChatMessageInput" cols="40" rows="3"></textarea>
    <input id="dhChatSendButton" type="button" value="Send" onclick="dh.chatwindow.sendClicked()"></input>
</body>
</html>
