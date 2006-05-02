<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Music Radar</title>
	<link rel="stylesheet" type="text/css" href="/css2/music.css"/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<c:if test="${signin.valid}">
		<dht:sidebarPerson who="${signin.user.id}">
			<dht:sidebarBoxControls title="MUSIC RADAR CONTROLS">
				<div>
					Music embed: <input id="dhMusicOn" type="radio"/> <label for="dhMusicOn">On</label> <input type="radio" id="dhMusicOff"/> <label for="dhMusicOff">Off</label>
				</div>
				<div style="margin-top: 3px;"><a href="/nowplaying-themes">Edit theme</a></div>
				<dht:sidebarBoxSeparator/>
				<div><a href="/music-bio">Edit my music bio</a></div>
			</dht:sidebarBoxControls>
		</dht:sidebarPerson>
	</c:if>
	<dht:contentColumn>
		<dht:zoneBoxMusic>
			<c:if test="${signin.valid}">
				<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>
			
				<dh:nowPlaying userId="${signin.user.id}" hasLabel="false"/>
			</c:if>

			<dht:zoneBoxSubcolumns>
				<c:if test="${signin.valid}">
					<dht:requireMusicPersonBean who="${signin.user.id}"/>
				</c:if>
				<dht:requireMusicGlobalBean/>

				<dht:zoneBoxSubcolumn which="one">

					<c:if test="${signin.valid}">
						<dht:zoneBoxTitle a="dhRecentSongs">MY RECENT SONGS</dht:zoneBoxTitle>
						
						<c:forEach items="${musicPerson.recentTracks.results}" var="track">
							<dht:track track="${track}" albumArt="true"/>
						</c:forEach>
	
						<dht:expandablePager pageable="${musicPerson.recentTracks}" anchor="dhRecentSongs"/>
	
						<dht:zoneBoxSeparator/>

						<dht:zoneBoxTitle a="dhMostPlayedSongs">MY MOST PLAYED SONGS</dht:zoneBoxTitle>
	
						<c:forEach items="${musicPerson.mostPlayedTracks.results}" var="track">
							<dht:track track="${track}"/>
						</c:forEach>
	
						<dht:expandablePager pageable="${musicPerson.mostPlayedTracks}" anchor="dhMostPlayedSongs"/>
	
						<dht:zoneBoxSeparator/>
					</c:if>

					<c:if test="${!signin.valid}">
						<dht:zoneBoxTitle>RECENT SONGS</dht:zoneBoxTitle>
						
						<c:forEach items="${musicGlobal.recentTracks.list}" var="track">
							<dht:track track="${track}" albumArt="true"/>
						</c:forEach>
	
						<dht:moreExpander open="false"/>
	
						<dht:zoneBoxSeparator/>
					</c:if>

					<dht:zoneBoxTitle>MOST PLAYED SONGS EVER</dht:zoneBoxTitle>

					<c:forEach items="${musicGlobal.mostPlayedTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>

					<dht:moreExpander open="false"/>

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
	
						<dht:moreExpander open="false"/>
						
						<dht:zoneBoxSeparator/>
	
						<dht:zoneBoxTitle a="dhFriendsRecentSongs">FRIENDS' RECENT SONGS</dht:zoneBoxTitle>
	
						<c:forEach items="${musicPerson.friendsRecentTracks.results}" var="track">
							<dht:track track="${track}"/>
						</c:forEach>

						<dht:expandablePager pageable="${musicPerson.friendsRecentTracks}" anchor="dhFriendsRecentSongs"/>
	
						<dht:zoneBoxSeparator/>
					</c:if>

					<c:if test="${!signin.valid}">
						<dht:zoneBoxTitle>MOST PLAYED SONGS TODAY</dht:zoneBoxTitle>
						
						<c:forEach items="${musicGlobal.mostPlayedToday.list}" var="track">
							<dht:track track="${track}"/>
						</c:forEach>
	
						<dht:moreExpander open="false"/>
	
						<dht:zoneBoxSeparator/>
					</c:if>

					<dht:zoneBoxTitle>ONE PLAY WONDERS</dht:zoneBoxTitle>

					<c:forEach items="${musicGlobal.onePlayTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>

					<dht:moreExpander open="false"/>
					
				</dht:zoneBoxSubcolumn>
			
			</dht:zoneBoxSubcolumns>
		</dht:zoneBoxMusic>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
