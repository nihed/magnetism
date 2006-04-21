<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Home</title>
	<link rel="stylesheet" type="text/css" href="/css2/home.css"/>
</head>
<dht:twoColumnPage>
	<dht:sidebar/>
	<dht:contentColumn>
		<dht:zoneBoxWeb more="true">
			<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
			<div class="dh-shared-link"><a href="">Talking pants all the rage with kids these days</a></div>
			<div class="dh-shared-link"><a href="">Just discovered awesome new kid-with-lightsaber vid</a></div>
			<div class="dh-shared-link"><a href="">Invest in kittens!</a></div>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>SHARED WITH ME</dht:zoneBoxTitle>
			<div class="dh-shared-link"><a href="">Space monkeys land in Harvard Square</a></div>
			<div class="dh-shared-link"><a href="">Government does shady business</a></div>
			<div class="dh-shared-link"><a href="">Biggest ball of wax you've ever seen</a></div>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic more="true">
			<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>
			<div class="dh-nowplaying"><div></div></div>
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
