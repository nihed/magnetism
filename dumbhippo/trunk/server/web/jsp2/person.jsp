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
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${person.viewedUserId}"/>
	<dht:contentColumn>
		<dht:zoneBoxWeb disableJumpTo="true">
			<dht:requireLinksPersonBean who="${person.viewedUserId}"/>
			<c:if test="${links.favoritePosts.size > 0}">
				<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
				<dht:postList posts="${links.favoritePosts.list}" format="simple"/>
				<dht:moreExpander open="false"/>
				<dht:zoneBoxSeparator/>
			</c:if>
			<dht:zoneBoxTitle>SHARED BY <c:out value="${fn:toUpperCase(person.viewedPerson.name)}"/></dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${links.sentPosts.size > 0}">
					<dht:postList posts="${links.sentPosts.list}" format="simple"/>
					<dht:moreExpander open="false"/>
				</c:when>
				<c:otherwise>
					Nothing shared by <c:out value="${person.viewedPerson.name}"/> yet!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic disableJumpTo="true">
			<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>
			<dh:nowPlaying userId="${person.viewedUserId}" hasLabel="false"/>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>RECENT SONGS</dht:zoneBoxTitle>
			<div class="dh-song"><a href="">Ice Ice Baby</a>
				<span class="dh-song-details">by <a href="">Vanilla Ice</a> | Play at <a href="">iTunes</a> | <a href="">Yahoo!</a></span>
			</div>
			<div class="dh-song"><a href="">Faraway</a>
				<span class="dh-song-details">by <a href="">Sleater-Kinney</a> | Play at <a href="">iTunes</a> | <a href="">Rhapsody</a></span>
			</div>							
		</dht:zoneBoxMusic>
		<dht:zoneBoxTv disableJumpTo="true">
			<dht:zoneBoxTitle>COMING SOON</dht:zoneBoxTitle>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
