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

		<dht:toolbar publicPageId="${invite.signin.userId}" home="true"/>
		
		<c:choose>
			<c:when test="${invite.invitations < 1}">
				<div id="dhInformationBar"><strong>You're Out Of Invites Right Now</strong></div>
			</c:when>
			<c:otherwise>
				<div id="dhInformationBar"><strong>You can invite up to ${invite.invitations} people to DumbHippo</strong></div>
			</c:otherwise>
		</c:choose>

		<c:if test="${(invite.invitations > 0) or (invite.validPreviousInvitation)}">
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
							<td>
						    <c:choose>
							    <c:when test="${(invite.invitations > 0)}">
							        <input name="email" class="dhText" autocomplete="off" value="${invite.email}">
							    </c:when>
							    <c:otherwise>
							        <input name="email" class="dhText" autocomplete="off" readonly="readonly" value="${invite.email}">
							    </c:otherwise>
							</c:choose>
							</td>
						</tr>
						<tr>
							<td>Subject:</td>
							<td><input name="subject" class="dhText" value="${invite.subject}"></td>
						</tr>							
					</table>
				</div>
				<div class="dh-invite-mail-body">
					<div class="dh-url">Click here to start using Dumb Hippo</div>
					<br/>
					<div>
						<textarea name="message" class="dhTextArea"><c:out value="${invite.message}"/></textarea>
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
		<div id="dhName"><dht:userNameEdit value="${invite.person.name}"/></div>
		</div>

		<div class="dh-right-box-area">
		<div class="dh-right-box dh-right-box">
			<h5 class="dh-title">People Who Need Invites</h5>
			<div class="dh-people">
			<!-- FIXME: need to figure out what we really want to display here -->
			<!-- perhaps just people who still need invites -->
			<!-- for some reason, it used to be checking groups, not contacts size, -->
			<!-- it should probably be contacts size, but it's such a mess what it -->
			<!-- displays if the condition is true, that it is better to leave it -->
			<!-- off for now; if this is enabled, more stuff from HomePage needs -->
			<!-- to be moved to AbstractSigninPage -->
			<c:choose>
				<c:when test="false"> <!-- this is the original condition ${home.groups.size > 0}" -->
					<dh:entityList value="${home.contacts.list}" showInviteLinks="${home.invitations > 0}" photos="true"/>
				</c:when>
				<c:otherwise>
					<!-- FIXME: need class definition for this -->
					<div class="dh-friends-none">You Need Peeps!!</div>
				</c:otherwise>
			</c:choose>
			</div>
		</div>
		<div class="dh-right-box dh-right-box-last">
			<h5 class="dh-title">Outstanding Invites</h5>
			<div class="dh-people">
            <dht:inviteList outstandingInvitations="${invite.outstandingInvitations}" invitesPage="false" start="0" maxInvitations="${invite.maxInvitationsShown}" totalInvitations="${invite.totalInvitations}"/>
			</div>
		</div>		
		</div>
		
	</div>

</div>

<div id="dhOTP">
<dht:rightColumn/>

</body>
</html>
