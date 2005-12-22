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
       <tr>
	       <td class="action" nowrap><a class="action highlight-action" onClick='dh.actions.requestJoinRoom("${framer.post.post.id}")' href="aim:GoChat?RoomName=${framer.post.chatRoomName}&Exchange=5"><dh:png klass="dh-chat-icon" src="/images/${buildStamp}/chat.png" style="width: 16; height: 16;"/></a></td>
	       <td class="action" nowrap><a class="action highlight-action" target="_blank" onClick='dh.actions.requestJoinRoom("${framer.post.post.id}")' href="aim:GoChat?RoomName=${framer.post.chatRoomName}&Exchange=5">${framer.post.chatRoomMembers}</a></td>
	   </tr>
	   <tr>
	       <td class="chat-preview" nowrap colspan=2>
             <c:choose>
	           <c:when test="${fn:length(chatmessages) > 0}">
	           What's going on in the chat:
	           <c:forEach items="${chatmessages}" var="chatmessage" varStatus="status">
	             <div class="chat-preview">
	                 <% /*  truncating this is a pain because the message text from AIM includes HTML markup */ %>
	                 ${chatmessage.fromScreenName}: ${chatmessage.messageText}
	             </div>
	           </c:forEach>
	         </c:when>
	         <c:otherwise>
	             <div class="chat-preview">
	               <!-- No chat messages, so far. -->
	             </div>
	         </c:otherwise>
	         </c:choose>      
	       </td>
	   </tr>
	   </table>
	</td>
	</tr>
	</table>
  </div>

</body>
</html>
