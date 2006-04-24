<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<dh:bean id="person" class="com.dumbhippo.web.PersonPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" param="who"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/site.css"/>
</head>
<dht:twoColumnPage>
	<dht:sidebar who="${person.viewedUserId}"/>
	<dht:contentColumn>
		<dht:zoneBoxWeb more="true">
			<dht:requireLinksBean who="${person.viewedUserId}"/>
			<c:if test="${links.favoritePosts.size > 0}">
				<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
					<dht:postList posts="${links.favoritePosts.list}" format="simple"/>
				<dht:zoneBoxSeparator/>
			</c:if>
			<dht:zoneBoxTitle>SHARED WITH <c:out value="${fn:toUpperCase(person.viewedPerson.name)}"/></dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${links.receivedPosts.size > 0}">
					<dht:postList posts="${links.receivedPosts.list}" format="simple"/>
				</c:when>
				<c:otherwise>
					Nothing shared with them yet!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic more="true">
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
		<dht:zoneBoxTv more="true">
			<dht:zoneBoxTitle>COMING SOON</dht:zoneBoxTitle>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
