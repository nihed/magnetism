<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="landing" class="com.dumbhippo.web.pages.LandingPage" scope="request"/>

<head>
	<title>Welcome to Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/landing.css"/>	
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
		dojo.require("dh.download");
	</script>
</head>
<dht:body>
	<div>
		<img src="/images2/${buildStamp}/mugshot_tagline.gif"/>
	</div>
    <%-- we are not offering this promotion any longer, so instead of invitesAvailable="${landing.selfInvitations}", --%>
    <%-- pretend we have no invites and offer people to "sign up"--%>
	<dht:selfInvite promotion="${landing.promotion}" invitesAvailable="0"/>
	<dht:notevil/>
	<div id="dhMusicRadarTag">
		<img src="/images2/${buildStamp}/musicradartag.gif"/>
	</div>
	<dht:beaconSamples/>
	<div class="dh-landing-explanatory">Easy to add to a blog or MySpace.  Select a theme or create your own.</div>
	<div id="dhLinkSwarmTag">
		<img src="/images2/${buildStamp}/linkswarmtag.gif"/>
	</div>
	<div id="dhLinkSwarmSample">
		<img src="/images2/${buildStamp}/bubblesample.gif"/>
	</div>
	<div class="dh-landing-explanatory">A desktop tool for Windows and Linux.  Instantly get the buzz on cool sites.</div>
</dht:body>
</html>
