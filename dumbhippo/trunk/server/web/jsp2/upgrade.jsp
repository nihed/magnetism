<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Upgrade</title>
	<link rel="stylesheet" type="text/css" href="/css2/upgrade.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage topText="A new version of Mugshot is available" logoOnly="true" disableJumpTo="true">
	<table cellspacing="0" cellpadding="0">
	<tr>
	<td><img id="dhUpgradeLogo" src="/images2/muggray60x60.png"/></td>
	<td>
	<p>Version 1.1.45</p>
	<ul>
		<li>Now with GObject, for smoother starts and better gas mileage</li>
	</ul>
	</td>
	</tr>		
	</table>
	<hr align="center" noshade="true" height="1px" width="80%" class="dh-gray-hr"/><br/>
	<div>
	<center>
	<input type="button" value="Install now" onclick="window.external.application.DoUpgrade(); window.close();"/> 
	<input type="button" value="Install later" onclick="window.close();"/>		
	</center>
	</div>
</dht:systemPage>	
</html>
