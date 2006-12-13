<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>

<div class="dh-favicons">
    <c:if test="${who.hasEmail && !empty who.email}">
        <dht3:whereAtIcon label="Send me email" linkText="${who.email.email}" linkTarget="${who.emailLink}" imgSrc="/images3/${buildStamp}/mail_icon.png"/>
	</c:if>
	<c:if test="${who.hasAim && !empty who.aim}">
		<c:set var="aimIcon" value="/images3/${buildStamp}/aim_icon.png"/>
		<c:if test="${!empty who.aimPresenceImageLink}">
			<c:set var="aimIcon" value="${who.aimPresenceImageLink}"/>
		</c:if>    
        <dht3:whereAtIcon label="AIM" linkText="${who.aim.screenName}" linkTarget="${who.aimLink}" imgSrc="${aimIcon}"/>
	</c:if>						
	<c:forEach var="account" items="${who.lovedAccounts.list}">
		<dht3:whereAtIcon label="${account.externalAccount.siteName}" linkText="${account.externalAccount.linkText}" linkTarget="${account.link}" imgSrc="/images3/${buildStamp}/${account.externalAccount.iconName}"/>
	</c:forEach>							
</div>