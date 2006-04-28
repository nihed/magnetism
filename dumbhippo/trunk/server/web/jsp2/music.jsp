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
	<dht:sidebarPerson who="${signin.user.id}">
		<dht:sidebarBoxControls title="MUSIC RADAR CONTROLS">
			<div>
				Music embed: <input type="radio"/> On <input type="radio"/> Off
			</div>
			<div style="margin-top: 3px;"><a href="/nowplaying-themes">Edit theme</a></div>
			<dht:sidebarBoxSeparator/>
			<div><a href="/music-bio">Edit my music bio</a></div>
		</dht:sidebarBoxControls>
	</dht:sidebarPerson>
	<dht:contentColumn>
		<dht:zoneBoxMusic>
			<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>
			
			<dh:nowPlaying userId="${signin.user.id}" hasLabel="false"/>

			<dht:zoneBoxSubcolumns>
				<dht:requireMusicPersonBean who="${signin.user.id}"/>
				<dht:requireMusicGlobalBean/>

				<dht:zoneBoxSubcolumn which="one">

					<dht:zoneBoxTitle>MY RECENT SONGS</dht:zoneBoxTitle>
					
					<c:forEach items="${musicPerson.recentTracks.list}" var="track">
						<dht:track track="${track}" albumArt="true"/>
					</c:forEach>

					<dht:moreExpander open="false"/>

					<dht:zoneBoxSeparator/>

					<dht:zoneBoxTitle>MY MOST PLAYED SONGS</dht:zoneBoxTitle>

					<c:forEach items="${musicPerson.mostPlayedTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>

					<dht:moreExpander open="false"/>

					<dht:zoneBoxSeparator/>

					<dht:zoneBoxTitle>MOST PLAYED SONGS EVER</dht:zoneBoxTitle>

					<c:forEach items="${musicGlobal.mostPlayedTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>

					<dht:moreExpander open="false"/>

				</dht:zoneBoxSubcolumn>
				<dht:zoneBoxSubcolumn which="two">
					
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

					<dht:zoneBoxTitle>FRIENDS' RECENT SONGS</dht:zoneBoxTitle>

					<dht:moreExpander open="false"/>

					<dht:zoneBoxSeparator/>

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
