<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Music Radar</title>
	<link rel="stylesheet" type="text/css" href="/css2/music.css"/>
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
			
			<dh:nowPlaying userId="c4a3fc1f528070" hasLabel="false"/>

			<dht:zoneBoxSubcolumns>

				<dht:zoneBoxSubcolumn which="one">

					<dht:zoneBoxTitle>SHARED WITH ME</dht:zoneBoxTitle>

					<div class="dh-item">
						<div class="dh-title"><a href="">Space Monkeys Land in Harvard Square, Buy Magazines</a></div>
						<div class="dh-blurb">Little green monkies were seen falling from the sky in Harvard Square
						yesterday afternoon. The ones that weren't trapped in trees converged at the newstands.
						</div>
					</div>
					<div class="dh-item">
						<div class="dh-title"><a href="">Government Invests in Kitten Farming</a></div>
						<div class="dh-blurb">Senators from all fifty states convinced President Bush to drop everything and 
						focus on the sudden kitten shortage currently gripping the country.
						</div>
					</div>
					
					<dht:moreExpander open="false"/>

				</dht:zoneBoxSubcolumn>
				<dht:zoneBoxSubcolumn which="two">
					
					<dht:zoneBoxTitle>SHARED BY ME</dht:zoneBoxTitle>
					<div class="dh-item">
						<div class="dh-title"><a href="">Space Monkeys Land in Harvard Square, Buy Magazines</a></div>
						<div class="dh-blurb">Little green monkies were seen falling from the sky in Harvard Square
						yesterday afternoon. The ones that weren't trapped in trees converged at the newstands.
						</div>
					</div>
					<div class="dh-item">
						<div class="dh-title"><a href="">Government Invests in Kitten Farming</a></div>
						<div class="dh-blurb">Senators from all fifty states convinced President Bush to drop everything and 
						focus on the sudden kitten shortage currently gripping the country.
						</div>
					</div>
					<div class="dh-item">
						<div class="dh-title"><a href="">HTML div needs to be longer</a></div>
						<div class="dh-blurb">Another item was needed in an HTML div to create unequal-length columns.
						</div>
					</div>									
					
					<dht:moreExpander open="false"/>

				</dht:zoneBoxSubcolumn>
			
			</dht:zoneBoxSubcolumns>
		</dht:zoneBoxMusic>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
