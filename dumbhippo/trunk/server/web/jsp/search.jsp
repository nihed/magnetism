<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="search" class="com.dumbhippo.web.SearchPage" scope="request"/>
<jsp:setProperty name="search" property="searchText" param="searchText"/>
<jsp:setProperty name="search" property="start" param="start"/>
<jsp:setProperty name="search" property="count" param="count"/>
<jsp:setProperty name="search" property="posterId" param="poster"/>
<jsp:setProperty name="search" property="recipientId" param="recipient"/>
<jsp:setProperty name="search" property="groupId" param="group"/>

<head>
	<title>Search</title>
	<dht:stylesheets href="search.css" iehref="bubbles-iefixes.css"/>
	<dht:scriptIncludes/>
</head>
<body>
<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

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
							<c:if test="${!search.signin.valid}">
								<dht:errorPage>Not signed in</dht:errorPage>
							</c:if>
							<jsp:setProperty name="search" property="recipientId" value="${search.signin.userId}"/>
							Search stuff shared with you:
						</c:otherwise>
					</c:choose>
					<input style="width:7em;border:1px solid black;" type="text" name="searchText" value="${search.searchText}"/>
				</p>
				<input type="hidden" name="start" value="${search.start}"/>
				<input type="hidden" name="count" value="${search.count}"/>
				<input type="hidden" name="poster" value="${search.posterId}"/>
				<input type="hidden" name="recipient" value="${search.recipientId}"/>
				<input type="hidden" name="group" value="${search.groupId}"/>
			</form>
		</div>
	
		<div>
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
	</div>
</div>
</body>
</html>
