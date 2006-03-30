<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.NowPlayingPage" scope="request"/>

<head>
        <title>Now Playing HTML Details</title>
        <dht:stylesheets />
        <dht:scriptIncludes/>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/>
	
		<dht:largeTitle>Now Playing HTML Options</dht:largeTitle>
		
		<div>
			If you're particular about your HTML you might want to know these details...
			otherwise <a href="/nowplaying">we have a simple version</a>
		</div>
		
		<dht:smallTitle>MySpace HTML</dht:smallTitle>
		
		<p>
		On MySpace, &lt;object&gt; tags aren't allowed, so you have to use the old-style
		&lt;embed&gt; tag as follows:
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea readonly="readonly" rows="7" wrap="off"><dh:nowPlaying userId="${nowplaying.signin.userId}" escapeXml="true" embedOnly="true"/></textarea>
		</div>

		<dht:smallTitle>Alternative HTML</dht:smallTitle>
		
		<p>
			Macromedia recommends HTML like this, which includes the standards-compliant &lt;object&gt; 
			tag with the old &lt;embed&gt; tag as a fallback. But again, MySpace won't allow this.
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea readonly="readonly" rows="17" wrap="off"><dh:nowPlaying userId="${nowplaying.signin.userId}" escapeXml="true"/></textarea>
		</div>
		
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>

