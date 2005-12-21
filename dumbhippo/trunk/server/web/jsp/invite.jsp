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

<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

		<dht:toolbar/>

		<c:choose>
			<c:when test="${invite.invitations < 1}">
				<div id="dhInformationBar">You're Out Of Invites Right Now</div>
			</c:when>
			<c:otherwise>
				<div id="dhInformationBar">You can invite up to ${invite.invitations} people to DumbHippo</div>
			</c:otherwise>
		</c:choose>


		<c:if test="${invite.invitations > 0}">
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
						This was sent to you by <dh:entity value="${invite.person}" photo="false"/> using <a href="/">Dumb Hippo</a>.
					</div>
				</div>
				<input class="dh-send-invite" type="submit" value="Send Invite"/>
			</form>
		</c:if>
	</div>

	<div id="dhPersonalArea">
		<div id="dhPhotoNameArea">
		<dht:headshot person="${invite.person}" size="192" />
		<dht:uploadPhoto location="/headshots" linkText="Change My Photo"/>
		<div id="dhName"><dht:userNameEdit value="${invite.person.name}"/></div>
		</div>

		<div class="dh-right-box-area">
		<div class="dh-right-box dh-right-box-last">
			<h5 class="dh-title">People Who Need Invites</h5>
			<div class="dh-people">
			<c:choose>
				<c:when test="${home.groups.size > 0}">
					<dh:entityList value="${home.contacts.list}" showInviteLinks="${home.invitations > 0}" photos="true"/>
				</c:when>
				<c:otherwise>
					<!-- FIXME: need class definition for this -->
					<div class="dh-friends-none">You Need Peeps!!</div>
				</c:otherwise>
			</c:choose>
			</div>
		</div>
		</div>
	</div>

</div>

<div id="dhOTP">
<dht:rightColumn/>
</div>

</body>
</html>
