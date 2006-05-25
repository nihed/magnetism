<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="landing" class="com.dumbhippo.web.LandingPage" scope="request"/>

<head>
	<title>Invite Yourself for a Mugshot</title>
    <link rel="stylesheet" type="text/css" href="/css2/landing.css"/>	
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
		dojo.require("dh.download");
	</script
</head>
<%-- TODO: could get a more appropriate header, but Sign Up works too. --%>
<dht:systemPage disableJumpTo="true" disableSignupLink="true" topImage="/images2/header_signup500.gif" fullHeader="true">
	
	<dht:zoneBoxTitle>Tell Us Your E-mail to Receive an Invitation</dht:zoneBoxTitle>
	<br/>
    <dht:selfInvite promotion="${landing.summitPromotion}" invitesAvailable="${landing.selfInvitations}" summitSelfInvite="true"/>
	
	<dht:zoneBoxSeparator/>
	
	<dht:zoneBoxTitle>Preview of Coming Attractions</dht:zoneBoxTitle>	
	<div id="dhLinkSwarmTag">
		<img src="/images2/linkswarmtag.gif"/>
	</div>
	<div id="dhLinkSwarmSample">
		<img src="/images2/bubblesample.gif"/>
	</div>
	<div class="dh-special-subtitle dh-landing-explanatory">
	    A desktop tool for Windows and Linux. Instantly get the buzz on cool sites.
	</div>
	<br/>
	<div id="dhMusicRadarTag">
		<img src="/images2/musicradartag.gif"/>
	</div>
	<dht:beaconSamples/>
	<div class="dh-special-subtitle dh-landing-explanatory">
	    Easy to add to a blog or MySpace. Select a theme or create your own.
	</div>
	
	<dht:zoneBoxSeparator/>

    <dht:notevil ownSection="true"/>
    
</dht:systemPage>
</html>