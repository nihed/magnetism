<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Sign In</title>
	<dht:stylesheets href="signin.css" />
</head>
<body>
	<dht:header>
		Sign In
	</dht:header>
	<!--  no toolbar, it's weird on this page -->
	
	<div id="dhMain">
		<form class="dh-signin-form" action="/signinpost" method="post">
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
							I have a <a href="/account">password</a>:
							<input type="password" class="dhText" name="password"/>
							<input type="submit" value="Sign in" name="checkpassword"/>
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
	</div>
</body>
</html>
