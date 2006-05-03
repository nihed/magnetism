<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="who" class="com.dumbhippo.web.WhoAreYouPage" scope="request"/>

<head>
	<title>Sign In</title>
	<link rel="stylesheet" type="text/css" href="/css2/who-are-you.css"/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		function dhEnterHandler(f) {
			return function(ev) {
				if (ev.keyCode == 13) {
					f()
					return false
				} else {
					return true
				}
			}
		}
		var dhEmailKeyPress = dhEnterHandler(function() {})
		var dhPasswordKeyPress = dhEnterHandler(function() {
			document.getElementById("dhPasswordButton").click()
		})
	</script>
</head>
<dht:body extraClass="dh-gray-background-page">
	<table style="width: 100%"><tr align="center"><td>
	<div id="dhHeaderLogo"><a href="/main"><img src="/images2/mugshot_logo.gif"/></a></div>	
	<dht:zoneBox zone="login" topImage="/images2/header_login500.gif" bottomImage="/images2/bottom_gray500.gif" disableJumpTo="true">
		<form id="dhSigninForm" action="/signinpost" method="post">
			<c:if test='${!empty param["next"]}'>
				<input type="hidden" value='${param["next"]}' name="next"/>
			</c:if>
			<table cellspacing="0px" cellpadding="0px" height="100%">
					<tr>
					<td>
					<table id="dhLoginNoPassword" height="100%" width="100%" valign="top" cellspacing="0px" cellpadding="0px">
						<tr><td class="dh-login-type-header">Log in without password</td></tr>
						<tr><td class="dh-login-text">Email address or AIM:</td></tr>
						<tr><td><input type="text" class="dhText" name="address" onkeypress="return dhEmailKeyPress(event)"/></td></tr>
						<tr><td><input id="dhSendLinkButton" type="submit" value="Send me a sign-in link" name="sendlink"/></td></tr>
					</table>
					</td>
					<td width="1px" style="background: gray;"><div></div></td>
					<td>
					<table id="dhLoginWithPassword" height="100%" width="100%" valign="top" cellspacing="0px" cellpadding="0px">
						<tr><td class="dh-login-type-header">Log in with password</td></tr>
						<tr><td class="dh-login-text">Email address or AIM:</td></tr>					
						<tr><td><input type="text" class="dhText" name="address" onkeypress="return dhEmailKeyPress(event)"/></td></tr>
						<tr><td class="dh-login-text">Password:</td></tr>
						<tr><td><input type="password" class="dhText" name="password" onkeypress="return dhPasswordKeyPress(event)"/></td></tr>	
						<tr><td><input id="dhPasswordButton" type="submit" value="Sign in with password" name="checkpassword"/></td></tr>
					</table>
					</td>
					</tr>
			</table>
		</form>
	</dht:zoneBox>
	<dht:footer/>	
	</td></tr></table>
</dht:body>
</html>
