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
	<dh:script modules="dh.login,dh.textinput"/>
</head>

<body>
	<gnome:page currentPageLink="who-are-you">
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
			            <p><strong>Enter your email address and we'll let you know when GNOME Online has more room. If you have a Mugshot account you can use it to log in.
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
