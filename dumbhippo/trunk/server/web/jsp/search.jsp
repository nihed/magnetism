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

	<c:if test="${!empty search.searchText}">
		<p>Your search is '${search.searchText}' but right now this page ignores that.</p>
	</c:if>

	<div id="dhMain">
		<div>
			<c:if test="${search.start > 0}">
				<a href="/search?${search.previousParams}">Newer posts</a>
			</c:if>
		</div>
			
		<div class="shared-links">
			<c:forEach items="${search.posts}" var="post" varStatus="status">
				<c:choose>
					<c:when test="${status.count == (search.count + 1)}">
						<!-- extra result is a marker that we have more coming, but don't 
							display it -->
						<div style="text-align:right">
							<a href="/search?${search.nextParams}">Later posts</a>
						</div>
					</c:when>
					<c:otherwise>
						<dht:postBubble post="${post}"/>
					</c:otherwise>
				</c:choose>
			</c:forEach>
		</div>
			
	</div>
</body>
</html>
