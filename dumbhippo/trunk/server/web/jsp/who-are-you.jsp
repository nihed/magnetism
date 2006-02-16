<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="who" class="com.dumbhippo.web.WhoAreYouPage" scope="request"/>

<head>
	<title>Sign In</title>
	<dht:stylesheets href="signin.css" />
</head>
<body>
<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>
	
		<form class="dh-signin-form" action="/signinpost" method="post">
			<c:if test='${!empty param["next"]}'>
				<input type="hidden" value='${param["next"]}' name="next"/>
			</c:if>
			<table>
				<tr class="dh-signin-address">
					<td>Email or AIM:</td>
					<td style="text-align: right;"><input type="text" class="dhText" name="address"/></td>
				</tr>
				<tr class="dh-signin-actions">
					<td colspan="2">
						<div>
							<input type="submit" value="Send me a sign-in link" name="sendlink"/>
						</div>
						<div>
							OR
						</div>
						<div>
							I might have a password:
							<input type="password" class="dhText" name="password"/>
							<input type="submit" value="Sign in with password" name="checkpassword"/>
						</div>
					</td>
				</tr>
			</table>
		</form>
		<c:if test="${param.wouldBePublic}">
			<p><b>Don't have an account?</b></p>
			<p>
				The page you're trying to get to will be public eventually, 
				but right now it's "members only." If you want in, leave us 
				your email address and we'll let you know when you can sign up.
			</p>
			<form method="post" action="/wantsin">
				<input type="text" value="let@me.in.please" name="address" class="dhText"/>
				<input type="submit" value="Want In?"/>
			</form>
		</c:if>
		<c:if test="${!who.browser.supported}">
			<p><b>Your Web Browser</b></p>
			Right now we've only tested the site with 
			<c:out value="${who.browser.supportedBrowsers}"/>. Your browser
			might work, but we haven't tried it ourselves.
			Feel free to give it a try and let us know if you find a problem.
			We'll test more browsers as soon as we can.
		</c:if>
	</div>
	<dht:bottom/>	
</div>
</body>
</html>
