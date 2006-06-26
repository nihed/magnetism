<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Suggested Links</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/links.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<c:if test="${signin.valid}">
	<dht:sidebarPerson who="${signin.user.id}">	
	</dht:sidebarPerson>
	</c:if>
	<dht:contentColumn>
		<dht:zoneBoxWeb>
			<c:choose>
				<c:when test="${signin.valid}">
					<dht:requireSuggestBean who="${signin.user.id}"/>
					<dht:zoneBoxTitle a="dhReceivedPosts">SUGGESTED LINKS</dht:zoneBoxTitle>
					<c:choose>
						<c:when test="${links.recommendedPosts.size > 0}">
							<dht:postList posts="${links.recommendedPosts.list}" format="full" favesMode="add-only"/>
						</c:when>
						<c:otherwise>
							No suggestions!
						</c:otherwise>
					</c:choose>
				</c:when>
				<c:otherwise><%-- not signed in case --%>
					No suggestions without login!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
