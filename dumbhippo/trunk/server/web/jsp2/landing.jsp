<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="landing" class="com.dumbhippo.web.LandingPage" scope="request"/>

<head>
	<title>Welcome to Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/landing.css"/>	
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
		dojo.require("dh.welcome");
	</script>
</head>
<body>
	<div id="dhLandingContainer">
	<div>
		<img src="/images2/mugshot_tagline.gif"/>
	</div>
	<dht:selfInvite promotion="${landing.promotion}" invitesAvailable="${landing.selfInvitations}"/>
	<dht:notevil/>
	<div id="dhMusicRadarTag">
		<img src="/images2/musicradartag.gif"/>
	</div>
	<dht:beaconSamples/>
	<div class="dh-landing-explanatory">Easy to add to a blog or MySpace.  Select a theme or create your own.</div>
	<div id="dhLinkSwarmTag">
		<img src="/images2/linkswarmtag.gif"/>
	</div>
	<div id="dhLinkSwarmSample">
		<img src="/images2/bubblesample.gif"/>
	</div>
	<div class="dh-landing-explanatory">A desktop tool for Windows.  Instantly get the buzz on cool sites.</div>	
	</div>
</body>
</html>
