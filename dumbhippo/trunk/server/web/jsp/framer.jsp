<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="postId"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>

<head>
	<title><c:out value="${title}"/></title>
	<link rel="stylesheet" href="/css/frames.css" type="text/css" />
	<dht:scriptIncludes/>
        <script type="text/javascript">
                dojo.require("dojo.html");
	</script>
</head>
<body>
	<div id="dhMain">
		<table>
		<tr>
		<td style="width:50%;">
		    <dht:postBubble post="${framer.post}"/>
		</td>
		<td style="width:30%;">
		    <a id="chat-room">Join Chat Room</a>
		</td>
		<td style="width:10%;">
		    <a href="${framer.post.url}" target=_top>Remove frame</a>
		</td>
		</tr>
		</table>
	</div>


<script>
a = document.getElementById("chat-room");
room = new String('<c:out value="${title}"/>' + ' Dumb Hippo');
result = room.replace(/\s/g, '');
a.href='aim:GoChat?Exchange=5&roomname=' + escape(result.slice(0,10));
</script>

</body>
</html>
