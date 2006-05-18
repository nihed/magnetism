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
	<link rel="stylesheet" type="text/css" href="/css2/music.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<c:if test="${signin.valid}">
		<dht:sidebarPerson who="${signin.user.id}">
			<dht:sidebarBoxControls title="MUSIC RADAR CONTROLS">
				<div>
					Music embed: 
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
					<div style="margin-top: 3px;"><a href="/radar-themes">Edit theme</a></div>
				</c:if>
				<dht:sidebarBoxSeparator/>
				<div><a href="/music-bio">Edit my music bio</a></div>
			</dht:sidebarBoxControls>
		</dht:sidebarPerson>
	</c:if>
	<dht:contentColumn>
		<dht:zoneBoxMusic>
			<c:choose>
				<c:when test="${signin.valid}">
					<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>		
					<dh:nowPlaying userId="${signin.user.id}" hasLabel="false"/>
				</c:when>
				<c:otherwise>
					<span class="dh-option-list">
					<a class="dh-option-list-option" href="/radar-learnmore">Learn More</a>
					|
					<a class="dh-option-list-option" href="/radar-themes">Browse themes</a>
					</span>
					<div><dht:beaconSamples/></div>					
				</c:otherwise>
			</c:choose>

			<dht:zoneBoxSubcolumns>
				<dht:zoneBoxSubcolumn which="one">

					<c:if test="${signin.valid}">

						<dht:trackList name="MY RECENT SONGS" id="dhRecentSongs" tracks="${musicGlobal.recentTracks.results}" albumArt="true" pageable="${musicGlobal.recentTracks}" separator="true"/>

						<dht:trackList name="MY MOST PLAYED SONGS" id="dhMostPlayedSongs" tracks="${musicGlobal.mostPlayedTracks.results}" pageable="${musicGlobal.mostPlayedTracks}" separator="true"/>
					</c:if>

					<c:if test="${!signin.valid}">

						<dht:trackList name="RECENT SONGS" id="dhRecentSongs" tracks="${musicGlobal.recentTracks.results}" pageable="${musicGlobal.recentTracks}" separator="true"/>

					</c:if>

					<dht:trackList name="MOST PLAYED SONGS EVER" id="dhGlobalMostPlayedSongs" tracks="${musicGlobal.mostPlayedTracks.results}" pageable="${musicGlobal.mostPlayedTracks}" />

				</dht:zoneBoxSubcolumn>
				<dht:zoneBoxSubcolumn which="two">
					
					<c:if test="${signin.valid}">
						<dht:zoneBoxTitle>MY MUSIC BIO</dht:zoneBoxTitle>
		
						<div class="dh-bio">				
							It all started when I was six years old, listening to the radio on 
							cross-country trips in the family car. Mom and Dad would sing drinking songs 
							to pass the hours, and before long I was singing along too. So now I enjoy 
							"normal" music like everyone else (MC Hammer, Spaghetti Arms Johnson, 
							Vanilla Ice) as well as class polkas and accordian standards. Suprisingly 
							I've never been asked to DJ at friends' parties.
						</div>
	
						<dht:zoneBoxSeparator/>
	
						<dht:trackList name="FRIENDS' RECENT SONGS" id="dhFriendsRecentSongs" tracks="${musicGlobal.friendsRecentTracks.results}" pageable="${musicGlobal.friendsRecentTracks}" separator="true"/>
					</c:if>

					<c:if test="${!signin.valid}">
						<dht:trackList name="MOST PLAYED SONGS TODAY" id="dhMostPlayedToday" tracks="${musicGlobal.mostPlayedToday.results}" pageable="${musicGlobal.mostPlayedToday}" separator="true"/>
					</c:if>

					<dht:trackList name="ONE PLAY WONDERS" id="dhOnePlayWonders" tracks="${musicGlobal.onePlayTracks.results}" pageable="${musicGlobal.onePlayTracks}" />
					
				</dht:zoneBoxSubcolumn>
			
			</dht:zoneBoxSubcolumns>
			<dht:zoneBoxSeparator/>
            <dht:webServicesAttributions/>     
		</dht:zoneBoxMusic>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
