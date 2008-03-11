<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Sign Up - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>
    <dht3:stylesheet name="signup"/>	
	<dht:faviconIncludes/>
</head>

<dh:bean id="landing" class="com.dumbhippo.web.pages.LandingPage" scope="request"/>

<dht3:page currentPageLink="signup">
    <dht3:pageSubHeader title="Sign Up"/>
    <dht3:shinyBox color="purple">  
    	<div id="dhSignupContent">	
    	    <div id="dhLoginLink"><a href="/who-are-you">Already have an account?</a></div>	
    	    <br/>
		    <c:choose>
		        <c:when test="${landing.selfInvitations > 0}">
                    <dht:selfInvite promotion="${landing.openSignupPromotion}" invitesAvailable="${landing.selfInvitations}"/>
		        </c:when>
		        <c:otherwise>
	                Enter your email address to request an invitation to Mugshot!
                    <dht:wantsIn buttonText="Sign Me Up!"/>
                </c:otherwise>
            </c:choose>		        
        <dht:zoneBoxSeparator/>
	    <div>
	        GNOME Online user? Use your <a href="http://online.gnome.org" target="_blank">online.gnome.org</a> account to <a href="/who-are-you">log in to Mugshot</a>.
	    </div>    	
	    <dht:zoneBoxSeparator/>
        <dht:notevil/>
        </div>
    </dht3:shinyBox>
</dht3:page>
</html>