<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Log In - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="who-are-you" iefixes="true"/>
	<dht:faviconIncludes/>
    <dh:script modules="dh.login"/>
</head>

<dh:bean id="whoareyou" class="com.dumbhippo.web.pages.WhoAreYouPage" scope="page"/>

<dht3:page currentPageLink="who-are-you">
    <dht3:pageSubHeader title="Log In"/>
    <dht3:shinyBox color="purple">
	    <form id="dhLoginForm" name="dhLoginForm" action="/signinpost" method="post">
		    <c:if test='${!empty param["next"]}'>
                <input type="hidden" value='${param["next"]}' name="next"/>
		    </c:if>	
            <div id="dhSignupLink"><a href="/signup">Don't have an account?</a></div>
		    <div id="dhLoginNotification" style="display: none;"></div>
		    <div>Email address:</div>
		    <div>
			    <dht:textInput id="dhLoginAddressEntry" name="address"/>
		    </div>
	        <div id="dhLoginPasswordLabel" style="display: none;">Password:</div>
	        <div>
			    <dht:textInput id="dhLoginPasswordEntry" type="password" name="password" style="display: none;"/>
	        </div>
		    <input id="dhLoginPasswordShowing" type="hidden" value='false' name="passwordShowing"/>
		    <div style="position:relative">
			    <input id="dhLoginButton" type="submit" value="Log in"/>
			    <a id="dhPasswordHelp" href="http://developer.mugshot.org/wiki/Did_You_Set_A_Password" target="_blank">Did you set a password?</a>
		    </div>
	        <dht:zoneBoxSeparator/>
	        <div>
		        <c:if test="${!empty whoareyou.aimBotScreenName}">
			        <%-- c:if on one line here to avoid weird link text spacing --%>
		            <a href="aim:GoIM?screenname=${whoareyou.aimBotScreenName}&message=Hey+Bot!+Send+me+a+login+link!" title="Get a log in link via AIM if you have already added AIM to your Mugshot account"><c:if test="${!empty whoareyou.aimPresenceKey}"><img src="http://api.oscar.aol.com/SOA/key=${whoareyou.aimPresenceKey}/presence/${whoareyou.aimBotScreenName}" border="0" style="vertical-align: text-bottom;"/> </c:if>AIM log in</a> |
		            <a href="http://apps.facebook.com/${whoareyou.facebookApplicationName}" title="Log in through Facebook if you have already added Facebook to your Mugshot account"><dh:png src="images3/${buildStamp}/favicon_facebook.png" style="width: 16; height: 16; vertical-align: text-bottom;"/> Facebook log in</a> |
	            </c:if>
		        <a id="dhLoginTogglePasswordLink" href="javascript:dh.login.togglePasswordBox()"><%-- filled in by javascript --%></a>&nbsp; [alt-p]
	        </div>
	        <dht:zoneBoxSeparator/>
	        <div>
	            GNOME Online user? Use your <a href="http://online.gnome.org" target="_blank">online.gnome.org</a> account to log in here.
	        </div>    
        </form>
    </dht3:shinyBox>
</dht3:page>
</html>