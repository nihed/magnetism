<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<dh:bean id="whoareyou" class="com.dumbhippo.web.pages.WhoAreYouPage" scope="page"/>

<head>
	<title>GNOME Online Desktop: Log In</title>
	<gnome:faviconIncludes/>
	<gnome:stylesheet name="site"/>
	<gnome:stylesheet name="who-are-you"/>	
	<dh:script module="dh.login"/>
</head>

<body>
	<form id="dhLoginForm" name="dhLoginForm" action="/signinpost" method="post">
		<c:if test='${!empty param["next"]}'>
			<input type="hidden" value='${param["next"]}' name="next"/>
		</c:if>	
		<table width="100%">
		<tr>
		  <td><div class="dh-title">Log in</div></td>
		  <td align="right"><div id="dhSignupLink"><a href="/signup">Don't have an account?</a></div></td>
		</tr>
		</table>
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
		<div style="position:relative">
			<input id="dhLoginButton" type="submit" value="Log in"/>
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
	
</body>
</html>
