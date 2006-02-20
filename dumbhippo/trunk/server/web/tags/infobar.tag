<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${!empty signin && !empty signin.user}">
	<c:choose>
		<c:when test="${signin.disabled}">
			<% /* FIXME: Seems ridiculous to show this instead of just forward them to the account page */ %>
			<div id="dhInformationBar"><a class="dh-information" href="/account">(re-enable your account)</a></div>
		</c:when>
		<c:when test="${browser.windows && !signin.user.account.wasSentShareLinkTutorial}">
			<div id="dhInformationBar"><a class="dh-information" href="/welcome">Download Dumbhippo Software Now</a></div>
		</c:when>
	</c:choose>
</c:if>
