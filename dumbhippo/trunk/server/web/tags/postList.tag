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
			<!-- extra result is a marker that we have more coming, but don't 
				display it -->
			<div style="text-align:right">
				<form style="display:inline;" action="/search">
					<input style="width:7em;border:1px solid black;" type="text" name="searchText" value="Search"/>
					<input type="hidden" name="start" value="0"/>
					<input type="hidden" name="count" value="${maxPosts}"/>
					<input type="hidden" name="posterId" value="${posterId}"/>
					<input type="hidden" name="recipientId" value="${recipientId}"/>
					<input type="hidden" name="groupId" value="${groupId}"/>
				</form>
				the <a href="/search?start=${maxPosts}&count=${maxPosts}&posterId=${posterId}&recipientId=${recipientId}&groupId=${groupId}">other shares</a>
				<c:choose>
					<c:when test="${!empty posterName}">
						<c:out value="${posterName}"/> shared.
					</c:when>
					<c:when test="${!empty groupName}">
						sent to the <c:out value="${groupName}"/> group.
					</c:when>
					<c:when test="${!empty recipientName}">
						you received.
					</c:when>
					<c:otherwise>
					</c:otherwise>
				</c:choose>
			</div>
		</c:when>
		<c:otherwise>
			<dht:postBubble post="${post}"/>
		</c:otherwise>
	</c:choose>
</c:forEach>
