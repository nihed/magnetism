<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%-- This is a temporary home for a replacement version of search.jsp that uses lucene --%>

<dh:bean id="find" class="com.dumbhippo.web.FindPage" scope="request"/>
<jsp:setProperty name="find" property="searchText" param="q"/>
<jsp:setProperty name="find" property="start" param="start"/>
<jsp:setProperty name="find" property="count" param="count"/>

<head>
	<title>Search</title>
	<dht:stylesheets href="search.css" iehref="bubbles-iefixes.css"/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
	</script>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/>
	
		<div>
			<form action="/find">
				<p>
					<input style="width:20em;border:1px solid black;" type="text" name="q" value="${find.searchText}"/>
				</p>
				<input type="hidden" name="start" value="${find.start}"/>
				<input type="hidden" name="count" value="${find.count}"/>
			</form>
		</div>
	
		<c:if test="${!empty find.searchText}">
			<div>
				<c:choose>
					<c:when test="${!empty find.error}">
						<div class="dhSearchError"><c:out value="${find.error}"/></div>
					</c:when>
					<c:when test="${find.posts.size == 0}">
						<div class="dhSearchError">Nothing Found</div>
					</c:when>
					<c:otherwise>
						<div>
							<c:if test="${find.start > 0}">
								<a href="/find?${find.previousParams}">Previous</a>
							</c:if>

							<c:out value="${1 + find.start}"/> to <c:out value="${1 + find.end}"/> out of
							<c:if test="${find.total > find.end}">
								about 
							</c:if>
							<c:out value="${find.total}"/>
							
							<c:if test="${find.end < find.total}">
								<a href="/find?${find.nextParams}">Next</a>
							</c:if>
						</div>
						<c:forEach items="${find.posts.list}" var="post">
							<div>
								<div><a href="/visit?postId=${post.post.id}"><c:out value="${post.title}"/></a></div>
								<div><c:out value="${post.text}"/></div>
							</div>
						</c:forEach>
					</c:otherwise>
				</c:choose>
			</div>
		</c:if>
	</dht:mainArea>
</dht:bodyWithAds>
</html>
