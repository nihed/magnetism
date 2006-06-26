<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.pages.NowPlayingPage" scope="request"/>

<head>
        <title>Get Music Radar</title>
        <link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/radar.css"/>
	<dht:faviconIncludes/>
        <dht:scriptIncludes/>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxMusic>
		
		<div><img src="/images2/${buildStamp}/musicradartag.gif"/></div>
		
		<div>
			If you're particular about your HTML you might want to know these details...
			otherwise we have <a href="/getradar">a simple version</a>.
		</div>		
		
		<p>
		On MySpace, &lt;object&gt; tags aren't allowed, so you have to use the old-style
		&lt;embed&gt; tag as follows:
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea class="dh-radar-code" readonly="readonly" rows="7" wrap="off"><dh:nowPlaying userId="${nowplaying.signin.userId}" escapeXml="true" embedOnly="true"/></textarea>
		</div>

		<p>
			Macromedia recommends HTML like this, which includes the standards-compliant &lt;object&gt; 
			tag with the old &lt;embed&gt; tag as a fallback. But again, MySpace won't allow this.
		</p>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea class="dh-radar-code" readonly="readonly" rows="17" wrap="off"><dh:nowPlaying userId="${nowplaying.signin.userId}" escapeXml="true"/></textarea>
		</div>		
	
		</dht:zoneBoxMusic>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>

