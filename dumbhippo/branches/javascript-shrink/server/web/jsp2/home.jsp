<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Mugshot Home</title>
	<dht:siteStyle/>	
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/home.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage disableHomeLink="true">
	<dht:sidebarPerson who="${signin.user.id}">
		<c:if test="${browser.windows}">
			<dht:sidebarBoxControls title="WINDOWS CONTROLS">
				<dht:actionLink href="/download" title="Download and install Mugshot for Windows to use Web Swarm and Music Radar">Download for Windows</dht:actionLink>
			</dht:sidebarBoxControls>
		</c:if>
		<c:if test="${browser.linux}">
			<dht:sidebarBoxControls title="LINUX CONTROLS">
				<dht:actionLink href="/download" title="Download and install Mugshot for Linux to use Web Swarm and Music Radar">Download for Linux</dht:actionLink>
			</dht:sidebarBoxControls>
		</c:if>
		<c:if test="${browser.gecko}">
			<dht:sidebarBoxControls title="FIREFOX CONTROLS">
				<dht:actionLink href="/bookmark" title="Add the Link Share Link to your Mozilla Firefox Browser">Add Mugshot to Firefox</dht:actionLink>
			</dht:sidebarBoxControls>
		</c:if>
		<c:if test="${browser.khtml}">
			<dht:sidebarBoxControls title="SAFARI CONTROLS">
				<dht:actionLink href="/bookmark" title="Add the Link Share Link to your Safari Browser">Add Mugshot to Safari</dht:actionLink>
			</dht:sidebarBoxControls>
		</c:if>
	</dht:sidebarPerson>
	<dht:contentColumn>
		<dht:zoneBoxWeb more="true">
		    <dht:linkSwarmPromo browserInstructions="true" linksLink="true" separator="true"/>	
			<dht:requireLinksPersonBean who="${signin.user.id}"/>
			<c:if test="${links.favoritePosts.resultCount > 0}">
				<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
					<dht:postList posts="${links.favoritePosts.results}" format="simple"/>
				<dht:zoneBoxSeparator/>
			</c:if>
			<c:choose>
				<c:when test="${links.receivedPosts.totalCount > 0}">
				    <dht:zoneBoxTitle>SHARED WITH YOU</dht:zoneBoxTitle>
					<dht:postList posts="${links.receivedPosts.results}" format="simple"/>
				</c:when>
				<c:otherwise>
					<dht:requireLinksGlobalBean/>
					<dht:zoneBoxTitle>RECENTLY SHARED</dht:zoneBoxTitle>
					<c:choose>
						<c:when test="${linksGlobal.hotPosts.resultCount > 0}">
							<dht:postList posts="${linksGlobal.hotPosts.results}" format="simple"/>
						</c:when>
						<c:otherwise>
							Nobody anywhere has ever shared anything!
						</c:otherwise>
					</c:choose>
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic more="true">
			<dht:musicRadarPromo separator="true"/>
			<c:if test="${signin.musicSharingEnabled}">
				<dht:requireMusicPersonBean who="${signin.user.id}"/>
				<dht:zoneBoxTitle>YOUR CURRENT SONG</dht:zoneBoxTitle>
				<dh:nowPlaying userId="${signin.user.id}" hasLabel="false"/>
				<dht:zoneBoxSeparator/>
				<dht:trackList name="FRIENDS' RECENT SONGS" tracks="${musicPerson.friendsRecentTracks.results}" separator="true" oneLine="true" displaySinglePersonMusicPlay="true" />
			</c:if>

			<dht:turnOnRadar/>

			<dht:requireMusicGlobalBean/>
			<dht:trackList name="MOST PLAYED SONGS TODAY" tracks="${musicGlobal.mostPlayedToday.results}" oneLine="true" />
		</dht:zoneBoxMusic>
		<dht:zoneBoxTv more="true">
			<dht:zoneBoxTitle>COMING SOON</dht:zoneBoxTitle>
			<p>We're actively working on our TV Party section, check our <a href="http://blog.mugshot.org/">blog</a> and the <a href="/tv">TV Party</a> page for updates.  Take a look at some of these blog entries for more details.</p>
			<ul>
			  <li><a href="http://blog.mugshot.org/?p=124">TV Dinner... Date?</a></li>
			  <li><a href="http://blog.mugshot.org/?p=107">Researching TV</a></li>
			  <li><a href="http://blog.mugshot.org/?p=84">Stick your TV shows on the wall</a></li>
			</ul>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
