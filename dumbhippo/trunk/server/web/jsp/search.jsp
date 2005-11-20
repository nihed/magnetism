<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="search" class="com.dumbhippo.web.SearchPage" scope="request"/>
<jsp:setProperty name="search" property="searchText" param="searchText"/>
<jsp:setProperty name="search" property="start" param="start"/>
<jsp:setProperty name="search" property="count" param="count"/>
<jsp:setProperty name="search" property="posterId" param="posterId"/>
<jsp:setProperty name="search" property="recipientId" param="recipientId"/>
<jsp:setProperty name="search" property="groupId" param="groupId"/>

<head>
	<title>Search</title>
	<dht:stylesheets href="/css/search.css" />
</head>
<body>
    <dht:header>Search</dht:header>
    
    <dht:toolbar/>

	<div>
		<form action="/search">
			<p>
				<c:choose>
					<c:when test="${search.poster != null}">
						Search stuff shared by ${search.poster.nickname}:
					</c:when>
					<c:when test="${search.recipient != null}">
						Search stuff shared with ${search.recipient.nickname}:
					</c:when>
					<c:when test="${search.group != null}">
						Search stuff from group ${search.group.name}:
					</c:when>
					<c:otherwise>
						Search:
					</c:otherwise>
				</c:choose>
				<input style="width:7em;border:1px solid black;" type="text" name="searchText" value="${search.searchText}"/>
			</p>
			<input type="hidden" name="start" value="${search.start}"/>
			<input type="hidden" name="count" value="${search.count}"/>
			<input type="hidden" name="posterId" value="${search.posterId}"/>
			<input type="hidden" name="recipientId" value="${search.recipientId}"/>
			<input type="hidden" name="groupId" value="${search.groupId}"/>
		</form>
	</div>

	<div id="dhMain">
		<div>
			<c:if test="${search.start > 0}">
				<a href="/search?${search.previousParams}">Newer posts</a>
			</c:if>
		</div>
			
		<div class="shared-links">
			<c:if test="${search.posts.size == 0}">
				<p>Nothing found</p>
			</c:if>
			<c:forEach items="${search.posts.list}" var="post" varStatus="status">
				<c:choose>
					<c:when test="${status.count == (search.count + 1)}">
						<!-- extra result is a marker that we have more coming, but don't 
							display it -->
						<div style="text-align:right">
							<a href="/search?${search.nextParams}">Later posts</a>
						</div>
					</c:when>
					<c:otherwise>
						<!-- don't display info that is already apparent from context -->
						<c:choose>
							<c:when test="${!empty search.posterId}">
								<dht:postBubble post="${post}" hidePoster="true"/>
							</c:when>
							<c:when test="${!empty search.recipientId}">
								<dht:postBubble post="${post}" hideRecipientId="${search.recipientId}"/>
							</c:when>
							<c:when test="${!empty search.groupId}">
								<dht:postBubble post="${post}" hideRecipientId="${search.groupId}"/>
							</c:when>
							<c:otherwise>
								<dht:postBubble post="${post}"/>
							</c:otherwise>
						</c:choose>
					</c:otherwise>
				</c:choose>
			</c:forEach>
		</div>
			
	</div>
</body>
</html>
