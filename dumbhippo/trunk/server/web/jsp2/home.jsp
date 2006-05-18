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
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage disableHomeLink="true">
	<dht:sidebarPerson who="${signin.user.id}">
	<c:choose>
		<c:when test="${browser.gecko}">
			<dht:sidebarBoxControls title="FIREFOX CONTROLS">
				<dht:actionLink href="/bookmark" title="Add the Link Share Link to your Mozilla Firefox Browser">Bookmark Page</dht:actionLink>
			</dht:sidebarBoxControls>
		</c:when>
		<c:when test="${browser.khtml}">
			<dht:sidebarBoxControls title="SAFARI CONTROLS">
				<dht:actionLink href="/bookmark" title="Add the Link Share Link to your Safari Browser">Bookmark Page</dht:actionLink>
			</dht:sidebarBoxControls>
		</c:when>
	</c:choose>
	</dht:sidebarPerson>
	<dht:contentColumn>
		<dht:zoneBoxWeb more="true">
			<dht:requireLinksPersonBean who="${signin.user.id}"/>
			<c:if test="${links.favoritePosts.resultCount > 0}">
				<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
					<dht:postList posts="${links.favoritePosts.results}" format="simple"/>
				<dht:zoneBoxSeparator/>
			</c:if>
			<c:choose>
				<c:when test="${links.receivedPosts.totalCount > 0}">
				    <dht:zoneBoxTitle>SHARED WITH ME</dht:zoneBoxTitle>
					<dht:postList posts="${links.receivedPosts.results}" format="simple"/>
				</c:when>
				<c:otherwise>
					<dht:requireLinksGlobalBean/>
					<dht:zoneBoxTitle>RECENTLY SHARED</dht:zoneBoxTitle>
					<c:choose>
						<c:when test="${linksGlobal.hotPosts.size > 0}">
							<dht:postList posts="${linksGlobal.hotPosts.list}" format="simple"/>
						</c:when>
						<c:otherwise>
							Nobody anywhere has ever shared anything!
						</c:otherwise>
					</c:choose>
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic more="true">
			<c:choose>
				<c:when test="${signin.musicSharingEnabled}">
					<dht:requireMusicPersonBean who="${signin.user.id}"/>
					<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>
					<dh:nowPlaying userId="${signin.user.id}" hasLabel="false"/>
					<dht:zoneBoxSeparator/>
					<dht:trackList name="MY RECENT SONGS" tracks="${musicPerson.recentTracks.results}" oneLine="true" />
				</c:when>
				<c:otherwise>
					<dht:requireMusicGlobalBean/>
					<table><tr>
					<td rowSpan="2"><img src="/images2/musicradar32x44.gif" /></td>
					<td><a style="font-weight:bold;" href="javascript:dh.actions.setMusicSharingEnabled(true);">Turn on Music Radar</a> to share your music taste with others.</td>
					</tr><tr>
					<td><a href="/radar-learnmore">Learn More</a></td>
					</tr></table>
					<dht:zoneBoxSeparator/>
					<dht:trackList name="MOST PLAYED SONGS TODAY" tracks="${musicGlobal.mostPlayedToday.results}" separator="true" oneLine="true" />
					<dht:trackList name="ONE PLAY WONDERS" tracks="${musicGlobal.onePlayTracks.results}" oneLine="true" />

				</c:otherwise>
			</c:choose>
		</dht:zoneBoxMusic>
		<dht:zoneBoxTv more="true">
			<dht:zoneBoxTitle>COMING SOON</dht:zoneBoxTitle>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
