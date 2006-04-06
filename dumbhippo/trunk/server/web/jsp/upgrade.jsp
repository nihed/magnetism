<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Upgrade DumbHippo!</title>
	<dht:stylesheets href="small-box.css"/>
	<dht:scriptIncludes/>
</head>
<dht:bodySmallBox>
	<dht:smallBoxTopArea>
		<h3>A new version of DumbHippo is ready to install!</h3>
		<i>Version 1.1.39:</i>
		<ul>
			<li>Several screens look much nicer</li>
			<li>Chat windows show everyone's currently-playing song</li>
			<li>Improved "Recent Links" feature in the tray icon menu</li>
			<li>Customizable themes for the "Now Playing" MySpace/blog embeddable flash</li>
			<li>Many speed improvements</li>
			<li>Many bug fixes</li>
		</ul>
		<input type="button" value="Start using the new stuff!" onclick="window.external.application.DoUpgrade(); window.close();"/> 
		<input type="button" value="No, not right now." onclick="window.close();"/>
	</dht:smallBoxTopArea>
</dht:bodySmallBox>
</html>
