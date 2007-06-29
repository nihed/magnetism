<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="landing" class="com.dumbhippo.web.pages.LandingPage" scope="request"/>

<head>
	<title>Sign up for Mugshot</title>
	<dht:siteStyle/>
    <link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/landing.css"/>	
	<dht:faviconIncludes/>
</head>
<%-- TODO: could get a more appropriate header, but Sign Up works too. --%>
<dht:systemPage disableJumpTo="true" currentPageLink="signup" topImage="/images2/${buildStamp}/header_signup500.gif" fullHeader="true">
	
	<dht:zoneBoxTitle>Sign up for Mugshot</dht:zoneBoxTitle>
	
	<div>
		
		<c:choose>
		    <c:when test="${landing.selfInvitations > 0}">
                <br/>
                <dht:selfInvite promotion="${landing.openSignupPromotion}" invitesAvailable="${landing.selfInvitations}"/>
		    </c:when>
		    <c:otherwise>
	            <p><strong>Enter your email address to request an invitation to Mugshot!
	            </strong></p>
                <dht:wantsIn buttonText="Sign Me Up!"/>
            </c:otherwise>
        </c:choose>
	</div>
	
	<dht:zoneBoxSeparator/>

    <dht:notevil ownSection="true"/>
    
</dht:systemPage>
</html>