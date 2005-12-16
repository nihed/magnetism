<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.PostView"%>
<%@ attribute name="hidePoster" required="false" type="java.lang.Boolean"%>
<%@ attribute name="hideRecipientId" required="false" type="java.lang.String"%>

<div class="cool-bubble-shadow">		
	<table class="cool-bubble">
	<tr>
	<c:if test="${!hidePoster}">
		<td class="cool-person" rowSpan="3">
			<a class="cool-person" href="">
				<dht:headshot person="${post.poster}" />
				<br/>
				<dh:entity value="${post.poster}" photo="false"/>&nbsp;<dh:presence value="${post.poster}"/>
			</a>
		</td>
	</c:if>
	<td class="cool-link">
		<div class="cool-link">
			<c:choose>
				<c:when test="${post.viewerHasViewed}">
					<c:set var="linkcss" value="viewed-link" scope="page"/>
				</c:when>
				<c:otherwise>
					<c:set var="linkcss" value="" scope="page"/>
				</c:otherwise>
			</c:choose>
			<a class="cool-link ${linkcss}" onClick="return dh.util.openFrameSet(window,event,this,'${post.post.id}');" title="${post.url}" href="${post.url}"><c:out value="${post.titleAsHtml}" escapeXml="false"/></a>&nbsp;<dh:presence value="${post}"/>
			<dh:favicon link="${post.url}"/>
		</div>
	</td>
	</tr>
	<tr>
	<td class="cool-link-desc">
		<c:out value="${post.textAsHtml}" escapeXml="false"/>
		<% /* <iframe src="chatroom?postId=${post.post.id}" width=300 height=50></iframe> */ %>
	</td>
	</tr>
	<tr>
	<td class="cool-link-meta">
	    <c:if test="${post.chatRoomActive}">
          <div class="cool-link-date"><a onClick='dh.actions.requestJoinRoom("${post.post.id}")' href="aim:GoChat?RoomName=${post.chatRoomName}&Exchange=5">${post.chatRoomMembers}</a></div>
        </c:if>
		<div class="cool-link-date">
		(<fmt:formatDate value="${post.post.postDate}" type="both"/>)
		</div>
		<div class="cool-link-to">
			<dh:entityList value="${post.recipients}" skipRecipientId="${hideRecipientId}"/>
		</div>
	</td>
	</tr>
	</table>
</div>
