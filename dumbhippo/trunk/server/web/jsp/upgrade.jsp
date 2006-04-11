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
		<i>Version 1.1.41:</i>
		<ul>
			<li>Chat messages appear in the notification bubble</li>
			<li>Client keeps track of what's going on currently better</li>			
			<li>Many bug fixes</li>
		</ul>
		<input type="button" value="Start using the new stuff!" onclick="window.external.application.DoUpgrade(); window.close();"/> 
		<input type="button" value="No, not right now." onclick="window.close();"/>
	</dht:smallBoxTopArea>
</dht:bodySmallBox>
</html>
