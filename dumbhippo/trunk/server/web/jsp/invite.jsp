<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="invite" class="com.dumbhippo.web.InvitePage" scope="request"/>

<head>
	<title>Invite a Friend</title>
	<dht:stylesheets />
	<dht:scriptIncludes/>
</head>
<body>
	<dht:header>
		Inviting a Friend
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<c:choose>
			<c:when test="${invite.invitations < 1}">
				<p>You have no more invitations available 
					right now.
				</p>
			</c:when>
			<c:otherwise>
				<p>You can invite up to ${invite.invitations} people
					to DumbHippo</p>
				<form action="/sendinvite" method="post">
					<h3>Invite someone by email:</h3>
					<table>
						<tr>
							<td>Name:</td>
							<td><input name="fullName"></td>
						</tr>
						<tr>
							<td>Email:</td>
							<td><input name="email"></td>
						</tr>
					</table>
					<input type="submit" value="Invite"/>
				</form>
			</c:otherwise>
		</c:choose>
	</div>
</body>
</html>
