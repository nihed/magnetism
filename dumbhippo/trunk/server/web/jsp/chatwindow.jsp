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

<c:url var="forwardUrl" value="/sharelink">
	<c:param name="url" value="${url}"/>
	<c:param name="title" value="${title}"/>
</c:url>

<head>
	<title><c:out value="${title}"/></title>
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
	<script type="text/javascript">
		var chatControl = document.getElementById("dhChatControl")
        if (chatControl && chatControl.readyState && chatControl.readyState == 4) {
			chatControl.Join(true)
			window.setTimeout(function() { chatControl.Rescan() }, 0)
		}
	</script>
	<script defer type="text/javascript">
		dh.chatwindow.init()
	</script>
</head>
<body scroll="no">
    <div id="dhChatPostInfoDiv">
    	${framer.post.titleAsHtml} (from <dh:entity value="${framer.post.poster}" photo="false"/>)
	</div>
    <div id="dhChatPeopleDiv"></div>
    <div id="dhChatAdsDiv">
        <div id="dhChatAdsInnerDiv">
        	<dht:ad src="${psa1}"/>
        </div>
    </div>
    <div id="dhChatMessagesDiv"></div>
    <textarea id="dhChatMessageInput" cols="40" rows="3"></textarea>
    <input id="dhChatSendButton" type="button" value="Send" onclick="dh.chatwindow.sendClicked()"></input>
</body>
</html>
