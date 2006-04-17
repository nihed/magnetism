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
		<i>Version 1.1.44:</i>
		<ul>
			<li>There is now a chat room for each group, in addition to the per-post chatrooms</li>
		</ul>
		<input type="button" value="Start using the new stuff!" onclick="window.external.application.DoUpgrade(); window.close();"/> 
		<input type="button" value="No, not right now." onclick="window.close();"/>
	</dht:smallBoxTopArea>
</dht:bodySmallBox>
</html>
