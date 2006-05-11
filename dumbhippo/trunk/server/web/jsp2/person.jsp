<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="person" class="com.dumbhippo.web.PersonPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" param="who"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${person.viewedUserId}"/>
	<dht:contentColumn>
		<dht:zoneBoxWeb disableJumpTo="true">
			<dht:requireLinksPersonBean who="${person.viewedUserId}"/>
			<c:if test="${links.favoritePosts.resultCount > 0}">
				<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
				<dht:postList posts="${links.favoritePosts.results}" format="simple"/>
				<dht:zoneBoxSeparator/>
			</c:if>
			<dht:zoneBoxTitle>SHARED BY <c:out value="${fn:toUpperCase(person.viewedPerson.name)}"/></dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${links.sentPosts.resultCount > 0}">
					<dht:postList posts="${links.sentPosts.results}" format="simple"/>
				</c:when>
				<c:otherwise>
					Nothing shared by <c:out value="${person.viewedPerson.name}"/> yet!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic disableJumpTo="true">
			<dht:requireMusicPersonBean who="${person.viewedUserId}"/>
			<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>
			<dh:nowPlaying userId="${person.viewedUserId}" hasLabel="false"/>
			
			<c:if test="${musicPerson.recentTracks.resultCount > 0}">
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle>RECENT SONGS</dht:zoneBoxTitle>
				
				<c:forEach items="${musicPerson.recentTracks.results}" var="track">
					<dht:track track="${track}" oneLine="true" playItLink="false"/>
				</c:forEach>
			</c:if>
		</dht:zoneBoxMusic>
		<dht:zoneBoxTv disableJumpTo="true">
			<dht:zoneBoxTitle>COMING SOON</dht:zoneBoxTitle>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
