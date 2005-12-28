<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.PostView"%>
<%@ attribute name="hidePoster" required="false" type="java.lang.Boolean"%>
<%@ attribute name="hideRecipientId" required="false" type="java.lang.String"%>


<div class="dh-share-shadow">
<div class="dh-share">
	<div class="dh-share-from">
		<dh:entity value="${post.poster}" photo="true" bodyLengthLimit="10"/>
	</div>
	<div class="dh-share-text">
		<a href="${post.url}" onClick="return dh.util.openFrameSet(window,event,this,'${post.post.id}');" title="${post.url}" class="dh-share-link"><c:out value="${post.titleAsHtml}" escapeXml="false"/></a>
		<dh:favicon link="${post.url}"/>
		<div class="dh-share-description"><c:out value="${post.textAsHtml}" escapeXml="false"/></div>
	</div>
	<div class="dh-share-to"><dh:entityList value="${post.recipients}" skipRecipientId="${hideRecipientId}" separator=", "/></div>
</div>
</div>


<% /*
<dh:presence value="${post.poster}"/>

	    <c:if test="${post.chatRoomActive}">
          <div class="cool-link-date"><dh:presence value="${post}"/><a onClick='dh.actions.requestJoinRoom("${post.post.id}")' href="aim:GoChat?RoomName=${post.chatRoomName}&Exchange=5">${post.chatRoomMembers}</a>
        </c:if>
*/ %>
