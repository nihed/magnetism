<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${signin.valid}">
	<dht:requireMusicPersonBean who="${signin.user.id}"/>
</c:if>
<dht:requireMusicGlobalBean/>

<head>
	<title>Mugshot Music Radar</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/music.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<c:if test="${signin.valid}">
		<dht:sidebarPerson who="${signin.user.id}">
			<dht:sidebarBoxControls title="MUSIC RADAR CONTROLS">
				<div>
					Music sharing: 
					<c:choose>
					<%-- this is duplicated so we can set the checked attribute...sigh --%>
					<c:when test="${signin.musicSharingEnabled}">
						<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled" checked="true" onclick="dh.actions.setMusicSharingEnabled(true);"> <label for="dhMusicOn">On</label>
						<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled" onclick="dh.actions.setMusicSharingEnabled(false);">	<label for="dhMusicOff">Off</label>			
					</c:when>
					<c:otherwise>
						<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled" onclick="dh.actions.setMusicSharingEnabled(true);"> <label for="dhMusicOn">On</label>
						<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled" checked="true" onclick="dh.actions.setMusicSharingEnabled(false);">	<label for="dhMusicOff">Off</label>
					</c:otherwise>
					</c:choose>
				</div>
				<c:if test="${signin.musicSharingEnabled}">				
					<div class="dh-music-radar-options" ><a href="/radar-themes">Edit theme</a></div>
					<div class="dh-music-radar-options" ><a href="/getradar">Get Music Radar HTML</a></div>
				</c:if>
				<dht:sidebarBoxSeparator/>
				<div><a href="/account">Edit your music bio</a></div>
			</dht:sidebarBoxControls>
		</dht:sidebarPerson>
	</c:if>
	<dht:contentColumn>
		<dht:zoneBoxMusic>
			<c:choose>
				<c:when test="${signin.valid}">
					<c:choose>
						<c:when test="${signin.musicSharingEnabled}">
							<dht:zoneBoxTitle>YOUR CURRENT SONG</dht:zoneBoxTitle>
							<dh:nowPlaying userId="${signin.user.id}" hasLabel="false"/>
						</c:when>
						<c:otherwise>
							<dht:turnOnRadar/>
						</c:otherwise>
					</c:choose>
				</c:when>
				<c:otherwise>
				        <dht:musicRadarPromo/>
					<div><dht:beaconSamples/></div>					
				</c:otherwise>
			</c:choose>

			<dht:zoneBoxSubcolumns>

				<dht:zoneBoxSubcolumn which="one">

					<c:if test="${signin.valid}">
						<dht:trackList name="FRIENDS' RECENT SONGS" id="dhFriendsRecentSongs" tracks="${musicPerson.friendsRecentTracks.results}" pageable="${musicPerson.friendsRecentTracks}" separator="true" albumArt="true" displaySinglePersonMusicPlay="true" />
					</c:if>

					<dht:trackList name="MOST PLAYED SONGS EVER" id="dhGlobalMostPlayedSongs" tracks="${musicGlobal.mostPlayedTracks.results}" pageable="${musicGlobal.mostPlayedTracks}" separator="true" />

					<dht:trackList name="ONE PLAY WONDERS" id="dhOnePlayWonders" tracks="${musicGlobal.onePlayTracks.results}" pageable="${musicGlobal.onePlayTracks}" />

				</dht:zoneBoxSubcolumn>
				<dht:zoneBoxSubcolumn which="two">
					<c:choose>
						<c:when test="${signin.valid}">
									
							<c:if test="${!empty person.viewedPerson.musicBioAsHtml}">
							    <dht:zoneBoxTitle>YOUR MUSIC BIO</dht:zoneBoxTitle>
							        <div class="dh-bio">
							            <c:out value="${person.viewedPerson.musicBioAsHtml}" escapeXml="false"/>
							        </div>
							    <dht:zoneBoxSeparator/>
							</c:if>
							
							<dht:trackList name="YOUR RECENT SONGS" id="dhRecentSongs" tracks="${musicPerson.recentTracks.results}" pageable="${musicPerson.recentTracks}" separator="true"/>
	
							<dht:trackList name="YOUR MOST PLAYED SONGS" id="dhMostPlayedSongs" tracks="${musicPerson.mostPlayedTracks.results}" pageable="${musicPerson.mostPlayedTracks}" />
		
						</c:when>
						<c:otherwise>
	
							<dht:trackList name="MOST PLAYED SONGS TODAY" id="dhMostPlayedToday" tracks="${musicGlobal.mostPlayedToday.results}" pageable="${musicGlobal.mostPlayedToday}" separator="true"/>
	
							<dht:trackList name="RECENT SONGS" id="dhRecentSongs" tracks="${musicGlobal.recentTracks.results}" pageable="${musicGlobal.recentTracks}" separator="true"/>
	
						</c:otherwise>
					</c:choose>					
				</dht:zoneBoxSubcolumn>
			
			</dht:zoneBoxSubcolumns>
		</dht:zoneBoxMusic>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
