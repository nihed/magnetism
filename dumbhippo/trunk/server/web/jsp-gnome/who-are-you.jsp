<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht2" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<dh:bean id="whoareyou" class="com.dumbhippo.web.pages.WhoAreYouPage" scope="page"/>
<dh:bean id="landing" class="com.dumbhippo.web.pages.LandingPage" scope="page"/>

<head>
	<gnome:title>Log In</gnome:title>
	<gnome:faviconIncludes/>
	<gnome:stylesheet name="site"/>
	<gnome:stylesheet name="who-are-you"/>	
	<dh:script module="dh.login"/>
</head>

<body>
	<gnome:page>
		<div style="float: left; width: 50%;">
			<div class="gnome-title">Log In to Your Account</div>
			<div style="height: 1em; width: 1px;"></div>
			<form id="dhLoginForm" name="dhLoginForm" action="/signinpost" method="post">
				<c:if test='${!empty param["next"]}'>
					<input type="hidden" value='${param["next"]}' name="next"/>
				</c:if>	
				<div id="dhLoginNotification" style="display: none;"></div>
				<div>Email address:</div>
				<div>
					<input id="dhLoginAddressEntry" name="address" type="text" maxlength="64"/>
				</div>
				<div id="dhLoginPasswordLabel" style="display: none;">Password:</div>
				<div>
					<input id="dhLoginPasswordEntry" type="password" name="password" style="display: none;" maxlength="64"/>
				</div>
				<input id="dhLoginPasswordShowing" type="hidden" value='false' name="passwordShowing"/>
				<div style="position:relative; margin-top: 10px;">
					<input id="dhLoginButton" type="submit" value="Login"/>
					<a id="dhPasswordHelp" href="http://developer.mugshot.org/wiki/Did_You_Set_A_Password" target="_blank">Did you set a password?</a>
				</div>
			</form>
			
			<div>
				<c:if test="${!empty whoareyou.aimBotScreenName}">
					<%-- c:if on one line here to avoid weird link text spacing --%>
					<a href="aim:GoIM?screenname=${whoareyou.aimBotScreenName}&message=Hey+Bot!+Send+me+a+login+link!"><c:if test="${!empty whoareyou.aimPresenceKey}"><img src="http://api.oscar.aol.com/SOA/key=${whoareyou.aimPresenceKey}/presence/${whoareyou.aimBotScreenName}" border="0"/> </c:if>AIM log in</a> |
				</c:if>
				<a id="dhLoginTogglePasswordLink" href="javascript:dh.login.togglePasswordBox()"><%-- filled in by javascript --%></a>&nbsp; [alt-p]
			</div>
		</div>
		
		<div style="float: left; width: 50%;">
			<div class="gnome-title">Sign Up</div>
			<div style="height: 1em; width: 1px;"></div>
			<div>
				<c:choose>
				    <c:when test="${landing.selfInvitations > 0}">
		                <dht2:selfInvite promotion="${landing.openSignupPromotion}" invitesAvailable="${landing.selfInvitations}"/>
				    </c:when>
				    <c:otherwise>
			            <p><strong>Enter your email address and we'll let you know when GNOME Online has more room.
			            </strong></p>
		                <dht2:wantsIn buttonText="Sign Me Up!"/>
		            </c:otherwise>
		        </c:choose>
			</div>
	
			<dht2:notevil ownSection="true"/>
		</div>
	</gnome:page>
</body>
</html>
