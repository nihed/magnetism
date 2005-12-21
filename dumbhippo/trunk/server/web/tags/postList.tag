<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="posts" required="true" type="java.util.List"%>
<%@ attribute name="maxPosts" required="true" type="java.lang.Integer"%>
<%@ attribute name="posterName" required="false" type="java.lang.String"%>
<%@ attribute name="recipientName" required="false" type="java.lang.String"%>
<%@ attribute name="groupName" required="false" type="java.lang.String"%>
<%@ attribute name="posterId" required="false" type="java.lang.String"%>
<%@ attribute name="recipientId" required="false" type="java.lang.String"%>
<%@ attribute name="groupId" required="false" type="java.lang.String"%>

<c:forEach items="${posts}" var="post" varStatus="status">
	<c:choose>
		<c:when test="${status.count == (maxPosts + 1)}">
			<% /*  extra result is a marker that we have more coming, but don't 
				display it */ %>
			<div class="dh-share-search">
				<form class="dh-share-search" action="/search">
					<input type="hidden" name="start" value="0"/>
					<input type="hidden" name="count" value="${maxPosts}"/>
					<input type="hidden" name="poster" value="${posterId}"/>
					<input type="hidden" name="recipient" value="${recipientId}"/>
					<input type="hidden" name="group" value="${groupId}"/>
					<input class="dh-share-search" type="text" name="searchText" value="Search"/> the <a class="dh-share-search" href="/search?start=${maxPosts}&count=${maxPosts}&poster=${posterId}&recipient=${recipientId}&group=${groupId}">other stuff</a>
				<c:choose>
					<c:when test="${!empty posterName}">
						<c:out value="${posterName}"/> shared
					</c:when>
					<c:when test="${!empty groupName}">
						sent to the <c:out value="${groupName}"/> group
					</c:when>
					<c:when test="${!empty recipientName}">
						you've seen
					</c:when>
					<c:otherwise>
					</c:otherwise>
				</c:choose>
				</form> 
			</div>
		</c:when>
		<c:otherwise>
			<% /* don't display info that is already apparent from context */ %>
			<c:choose>
				<c:when test="${!empty posterId}">
					<dht:postBubble post="${post}" hidePoster="true"/>
				</c:when>
				<c:when test="${!empty recipientId}">
					<dht:postBubble post="${post}" hideRecipientId="${recipientId}"/>
				</c:when>
				<c:when test="${!empty groupId}">
					<dht:postBubble post="${post}" hideRecipientId="${groupId}"/>
				</c:when>
				<c:otherwise>
					<dht:postBubble post="${post}"/>
				</c:otherwise>
			</c:choose>
		</c:otherwise>
	</c:choose>
</c:forEach>
