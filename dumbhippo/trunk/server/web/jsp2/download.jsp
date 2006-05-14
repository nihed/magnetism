<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="welcome" class="com.dumbhippo.web.WelcomePage" scope="request"/>

<head>
	<title>Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/download.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>

<dht:body>
	<img src="/images2/mugshot_tagline.gif"/>
	<div class="dh-special-subtitle">Thanks for trying us out!  Here's how to start using our tools...</div>
	<table cellspacing="15px" cellpadding="0" align="center">
	<tr valign="top">
	<td>
		<table cellspacing="0" cellpadding="0">
			<tr><td><img id="dhDownloadImg" src="/images2/buzzer63x58.gif"/></td>
			<td class="dh-download-instructions">1. <a class="dh-download-product" href="${welcome.downloadUrlWindows}">Click here to download</a>.<br/>
			    The software will install automatically.</td>
			</tr>
		</table>
	</td>
	<td class="dh-download-separator"><div></div></td>
	<td class="dh-download-instructions"><center>
	<div>2. Open iTunes or Yahoo! Music and play a song.</div>
	<div><img src="/images2/musicradar45x57.gif"/></div></center>
	</td>
	<td class="dh-download-separator"><div></div></td>	
	<td class="dh-download-instructions">
	3. Click the link on the bubble that appears.<br/>
	<img src="/images2/minibubble.gif"/>
	</td>	
	</tr>
	</table>
	<div class="dh-special-subtitle">Clicking that link will activate Link Swarm, and take you
	to our page where you can get and customize Music Radar for your own page.  Easy!</div>
	<dht:notevil/>
</dht:body>

</html>
