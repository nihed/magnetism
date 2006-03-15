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
	
		<h2>Now Playing</h2>
		
		<p>
		&nbsp;
		</p>
		
		<div>
			<c:out value="${nowplaying.nowPlayingObjectHtml}" escapeXml="false"/>
		</div>
		
		<h2>MySpace HTML</h2>
		
		<p>
		To put the above "now playing" on MySpace, cut-and-paste this HTML:
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea readonly="readonly" rows="7" wrap="off"><c:out value="${nowplaying.nowPlayingEmbedHtml}" escapeXml="true"/></textarea>
		</div>

		<h2>Alternative HTML</h2>
		
		<p>
		The below HTML may improve browser compatibility, but won't work on MySpace.
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea readonly="readonly" rows="17" wrap="off"><c:out value="${nowplaying.nowPlayingObjectHtml}" escapeXml="true"/></textarea>
		</div>
		
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>

