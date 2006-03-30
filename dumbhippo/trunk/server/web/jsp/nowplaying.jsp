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
		
		&nbsp;
		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}"/>
		</div>
		
		<div style="text-align: right;">
			<a href="/nowplaying-themes">More Themes</a>
		</div>
		
		<dht:largeTitle>HTML</dht:largeTitle>
		
		<p>
		To put the above "now playing" on MySpace or your blog, cut-and-paste this HTML:
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea readonly="readonly" rows="7" wrap="off"><dh:nowPlaying userId="${nowplaying.signin.userId}" escapeXml="true" embedOnly="true"/></textarea>
		</div>

		<div>
			You can change your theme without changing the HTML, just
			<a href="/nowplaying-themes">pick a different theme</a> at any 
			time and it will immediately go live on any site where you're using the 
			"now playing" embed.
		</div>

		&nbsp;

		<div>
			If you're an HTML geek and you want to see other ways to do the embed, 
			<a href="/nowplaying-html">here are some technical details</a>.
		</div>
		
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>

