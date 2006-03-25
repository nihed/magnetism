<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.NowPlayingPage" scope="request"/>

<head>
        <title>Now Playing</title>
        <dht:stylesheets />
        <dht:scriptIncludes/>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/>
	
		<dht:largeTitle>Now Playing</dht:largeTitle>
		
		<p>
		&nbsp;
		</p>
		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}"/>
		</div>
		
		<dht:largeTitle>MySpace HTML</dht:largeTitle>
		
		<p>
		To put the above "now playing" on MySpace, cut-and-paste this HTML:
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea readonly="readonly" rows="7" wrap="off"><dh:nowPlaying userId="${nowplaying.signin.userId}" escapeXml="true"/></textarea>
		</div>

		<dht:largeTitle>Alternative HTML</dht:largeTitle>
		
		<p>
		The below HTML may improve browser compatibility, but won't work on MySpace.
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea readonly="readonly" rows="17" wrap="off"><dh:nowPlaying userId="${nowplaying.signin.userId}" escapeXml="true"/></textarea>
		</div>
		
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>

