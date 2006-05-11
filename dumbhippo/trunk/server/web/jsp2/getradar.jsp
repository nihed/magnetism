<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.NowPlayingPage" scope="request"/>

<head>
        <title>Get Music Radar</title>
        <link rel="stylesheet" type="text/css" href="/css2/radar.css"/>
        <dht:scriptIncludes/>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxMusic>
		
		<div><img src="/images2/musicradartag.gif"/></div>
	
		<dht:beaconSamples/>
		
		<div>
			<a href="/radar-themes">Browse and edit themes</a> to change how your Music Radar looks.
		</div>
		
		<dht:zoneBoxSeparator/>		
	
		<dht:zoneBoxTitle>HOW TO PUT MUSIC RADAR ON YOUR MYSPACE PAGE</dht:zoneBoxTitle>
		
		<ul>
			<li>Copy the HTML code in the box below</li>
			<li>On your MySpace <i>Edit Profile</i> page, click <b>Edit</b> for the <i>About Me</i> section.</li>
			<li>Paste the code anywhere in the <i>About Me</i> box, and click <b>Preview</b>.</li>
			<li>On the <i>Preview</i> page, click <b>Submit</b></li>
		</ul>
		
		<div>
			<!--  don't add whitespace inside the textarea tag -->
			<textarea class="dh-radar-code" readonly="readonly" rows="7" wrap="off"><dh:nowPlaying userId="${nowplaying.signin.userId}" escapeXml="true" embedOnly="true"/></textarea>
		</div>

		<div>
			You don't need to change any code to give your Music Radar a new look.  Simply
			<a href="/radar-themes">Browse and Edit themes</a> and your changes will
			update instantly.
		</div>

		<div>
			If you're an HTML geek, <a href="/radar-html">here are some technical details</a>.
		</div>	
		</dht:zoneBoxMusic>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>

