<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="mail" class="com.dumbhippo.web.pages.MailSettingsPage" scope="request"/>

<head>
	<title>Mail Settings</title>
	<dht:stylesheets />
</head>
<dht:body>
	<dht:mainArea>
		<dht:toolbar/>

		<c:choose>
			<c:when test="${mail.signin.valid}">
				<p>You're currently signed in as '${mail.person.name}'.
				You can enable or disable your account on 
				<a href="/account">this page</a>. If you disable
				your account, you won't receive email from us.
				</p>
			</c:when>
			<c:when test="${!empty mail.email}">
				<p>
					<c:choose>
						<c:when test="${mail.enabled}">
							Email to '${mail.email}' is enabled.
							We'll send mail to you at '${mail.email}' if your
							friends share links, photos, or other items with you.
							<br/><a href="${mail.disableLink}">Disable email to '${mail.email}'</a>
						</c:when>
						<c:otherwise>
							Email to '${mail.email}' is disabled.
							We won't notify you at '${mail.email}' if your
							friends share links, photos, or other items with you.
							<br/><a href="${mail.enableLink}">Enable email to '${mail.email}'</a>
						</c:otherwise>
					</c:choose>
				</p>
				<p>We will never send you spam or sell your email address.
				If you enable email, your address will only be used when 
				people share links, photos, or other items with you.
				</p>
			</c:when>
			<c:otherwise>
				<p>
				This page only works if you reach it by clicking on the
				"stop getting these emails" links included at the bottom
				of each email we send.
				</p>
			</c:otherwise>
		</c:choose>		
	<br/>
	<p><a href="/privacy">Our privacy policy</a></p>
	</dht:mainArea>
	<dht:bottom/>
</dht:body>
</html>
