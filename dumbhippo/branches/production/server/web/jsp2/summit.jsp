<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="landing" class="com.dumbhippo.web.pages.LandingPage" scope="request"/>

<head>
	<title>Invite Yourself for a Mugshot</title>
    <link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/landing.css"/>	
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
		dojo.require("dh.download");
	</script
</head>
<%-- TODO: could get a more appropriate header, but Sign Up works too. --%>
<dht:systemPage disableJumpTo="true" disableSignupLink="true" topImage="/images2/${buildStamp}/header_signup500.gif" fullHeader="true">
	
	<dht:zoneBoxTitle>Invite Yourself for a Mugshot</dht:zoneBoxTitle>
	
	<div>
		<br/>
		<%-- we are not offering this promotion any longer, so instead of invitesAvailable="${landing.selfInvitations}", --%>
		<%-- pretend we have no invites and offer people to "sign up" --%>	
	    <dht:selfInvite promotion="${landing.summitPromotion}" invitesAvailable="0" summitSelfInvite="true"/>
	</div>
	
	<dht:zoneBoxSeparator/>
	
	<dht:zoneBoxTitle>Preview of Coming Attractions</dht:zoneBoxTitle>	
	<div id="dhLinkSwarmTag">
		<img src="/images2/${buildStamp}/linkswarmtag.gif"/>
	</div>
	<div id="dhLinkSwarmSample">
		<img src="/images2/${buildStamp}/bubblesample.gif"/>
	</div>
	<div class="dh-special-subtitle dh-landing-explanatory">
	    A desktop tool for Windows and Linux. Instantly get the buzz on cool sites.
	</div>
	<br/>
	<div id="dhMusicRadarTag">
		<img src="/images2/${buildStamp}/musicradartag.gif"/>
	</div>
	<dht:beaconSamples/>
	<div class="dh-special-subtitle dh-landing-explanatory">
	    Easy to add to a blog or MySpace. Select a theme or create your own.
	</div>
	
	<dht:zoneBoxSeparator/>

    <dht:notevil ownSection="true"/>
    
</dht:systemPage>
</html>
