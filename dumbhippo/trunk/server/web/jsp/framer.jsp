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
<c:set var="chatmessages" value="${framer.post.lastFewChatRoomMessages}" scope="page"/>

<c:url var="forwardUrl" value="/sharelink">
	<c:param name="url" value="${url}"/>
	<c:param name="title" value="${title}"/>
</c:url>

<head>
	<title><c:out value="${title}"/></title>
	<dht:stylesheets href="frames.css" iehref="frames-iefixes.css" />
	<dht:scriptIncludes/>
    <script type="text/javascript">
    	dojo.require("dojo.html");
	</script>
</head>
<body>
   <div id="dhMain">
	<table class="dhFramer">
	<tr>
	<td rowSpan="3">
		<dht:headshot person="${framer.post.poster}"/>
	</td>
	<td class="cool-link-desc">
	     <div class="cool-link"><c:out value="${title}" />&nbsp;<dh:presence value="${framer.post}"/></div>
	     <div class="cool-link-desc"><c:out value="${description}" /></div>
	     <div class="cool-link-chat">
	     <c:choose>
	       <c:when test="${fn:length(chatmessages) > 0}">
	         What's going on in the chat:
	         <c:forEach items="${chatmessages}" var="chatmessage" varStatus="status">
	           <div class="cool-link-chat-mesg">
	             <% /*  truncating this is a pain because the message text from AIM includes HTML markup */ %>
	             ${chatmessage.fromScreenName}: ${chatmessage.messageText}
	           </div>
	         </c:forEach>
	       </c:when>
	       <c:otherwise>
	         <div class="cool-link-chat-none">No chat messages.</div>
	       </c:otherwise>
	     </c:choose>
	     <a class="join-chat" onClick='dh.actions.requestJoinRoom("${framer.post.post.id}")' href="aim:GoChat?RoomName=${framer.post.chatRoomName}&Exchange=5">Join</a>
    	 </div>
	</td>
	<td class="action-area">
	   <table class="action-area" cellspacing="2px">
	   <tr>
	       <td class="action" nowrap><a class="action action-box" href="${url}" target=_top>X</a></td>
	       <td class="action" nowrap><a class="action" href="${url}" target=_top>Remove Frame</a></td>
	   </tr>
	   <tr>
	       <td class="action" nowrap><a class="action action-box" href="/home" target=_top>&#171;</a></td>	
	       <td class="action" nowrap><a class="action" href="/home" target=_top>Back Home</a></td>       
	   </tr>
	   <tr>
	       <td class="action" nowrap><a class="action action-box highlight-action" href="${forwardUrl}" target="_blank">&#187</a></td>
	       <td class="action" nowrap><a class="action highlight-action" href="${forwardUrl}" target="_blank">Forward To Others</a></td>
	    </tr>
	    </table>
	</td>
	</tr>
	<tr>
	   <td> 
	   </td>
	   <td class="cool-link-meta">
	     <div class="cool-link-date"><a onClick='dh.actions.requestJoinRoom("${framer.post.post.id}")' href="aim:GoChat?RoomName=${framer.post.chatRoomName}&Exchange=5">${framer.post.chatRoomMembers}</a></div>
	     <div class="cool-link-to">This was sent to you by <dh:entity value="${framer.post.poster}" photo="false"/>&nbsp;<dh:presence value="${post.poster}"/></div>
	   </td>
	</tr>
	</table>
  </div>

</body>
</html>
