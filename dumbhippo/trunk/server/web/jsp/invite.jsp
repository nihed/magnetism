<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="invite" class="com.dumbhippo.web.InvitePage" scope="request"/>
<jsp:setProperty name="invite" property="email" param="email"/>

<head>
	<title>Invite a Friend</title>
	<dht:stylesheets href="invite.css"/>
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
					<div class="dh-invite-mail-header">
						<table>
							<tr>
								<td>From:</td>
								<td><c:out value="${invite.person.name}"/> &lt;<c:out value="${invite.person.email.email}"/>&gt;
								</td>
							</tr>
							<tr>
								<td>To:</td>
								<td><input name="email" class="dhText" autocomplete="off" value="${invite.email}">
								</td>
							</tr>
							<tr>
								<td>Subject:</td>
								<td><input name="subject" class="dhText" value="Invitation from ${invite.person.name} to use Dumb Hippo"></td>
							</tr>							
						</table>
					</div>
					<div class="dh-invite-mail-body">
						<div class="dh-url">Click here to start using Dumb Hippo</div>
						<br/>
						<div>
							<textarea name="message" class="dhTextArea">[your message goes here]</textarea>
						</div>
						<br/>
						<div>
							This was sent to you by <dh:entity value="${invite.person}"/> using <a href="/">Dumb Hippo</a>.
						</div>
					</div>
					<input type="submit" value="Send Invite"/>
				</form>
			</c:otherwise>
		</c:choose>
	</div>
</body>
</html>
