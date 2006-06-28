<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="whoareyou" class="com.dumbhippo.web.pages.WhoAreYouPage" scope="page"/>

<head>
	<title>Log In</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/who-are-you.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.login");
	</script>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/${buildStamp}/header_login310.gif" bottomImage="/images2/${buildStamp}/bottom_gray310.gif">
	<form id="dhLoginForm" name="dhLoginForm" action="/signinpost" method="post">
		<c:if test='${!empty param["next"]}'>
			<input type="hidden" value='${param["next"]}' name="next"/>
		</c:if>				
		<div class="dh-title">Log in</div>
		<div id="dhLoginNotification" style="display: none;"></div>
		<div>Email address:</div>
		<div>
			<dht:textInput name="address"/>
		</div>
		<div id="dhLoginPasswordLabel" style="display: none;">Password:</div>
		<div>
			<dht:textInput id="dhLoginPasswordEntry" type="password" name="password" style="display: none;"/>
		</div>
		<input id="dhLoginPasswordShowing" type="hidden" value='false' name="passwordShowing"/>
		<div>
			<input id="dhLoginButton" type="submit" value="Log in"/>
		</div>
	</form>
	<dht:zoneBoxSeparator/>
	<div>
		<c:if test="${!empty whoareyou.aimBotScreenName}">
			<%-- c:if on one line here to avoid weird link text spacing --%>
			<a href="aim:GoIM?screenname=${whoareyou.aimBotScreenName}&message=Hey+Bot!+Send+me+a+login+link!"><c:if test="${!empty whoareyou.aimPresenceKey}"><img src="http://api.oscar.aol.com/SOA/key=${whoareyou.aimPresenceKey}/presence/${whoareyou.aimBotScreenName}" border="0"/> </c:if>AIM log in</a> |
		</c:if>
		<a id="dhLoginTogglePasswordLink" href="javascript:dh.login.togglePasswordBox()"><%-- filled in by javascript --%></a>&nbsp; [alt-p]
	</div>
	
</dht:systemPage>
</html>
