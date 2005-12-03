<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
		<dht:headshot personId="${framer.post.poster.viewPersonPageId}"/>
	</td>
	<td class="cool-link-desc">
	     <div class="cool-link"><c:out value="${title}" /></div>
	     <div class="cool-link-desc"><c:out value="${description}" /></div>
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
	   <td><div class="join-chat"><a class="join-chat" onClick='dh.actions.requestJoinRoom("${framer.chatRoom}")' href="aim:GoChat?RoomName=${framer.chatRoom}&Exchange=5">Join Chat Room</a></div></td>

	   <td class="cool-link-meta">
	     <div class="cool-link-date">${framer.chatRoomMembers}</div>
	     <div class="cool-link-to">This was sent to you by <dh:entity value="${framer.post.poster}" photo="false"/></div>
	   </td>
	</tr>
	</table>
  </div>

</body>
</html>
