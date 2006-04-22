<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Mugshot Home</title>
	<link rel="stylesheet" type="text/css" href="/css2/home.css"/>
</head>
<dht:twoColumnPage>
	<dht:sidebar who="${signin.user.id}"/>
	<dht:contentColumn>
		<dht:zoneBoxWeb more="true">
			<dh:bean id="links" class="com.dumbhippo.web.LinksPage" scope="page"/>
			<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
			FIXME
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>SHARED WITH ME</dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${links.receivedPosts.size > 0}">
					<dht:postList posts="${links.receivedPosts.list}"/>
				</c:when>
				<c:otherwise>
					Nothing shared with you yet!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic more="true">
			<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>
			<dh:nowPlaying userId="${signin.user.id}" hasLabel="false"/>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>MY RECENT SONGS</dht:zoneBoxTitle>
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
