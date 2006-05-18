<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="hideEnableAccount" required="false" type="java.lang.Boolean" %>

<c:if test="${!empty signin && !empty signin.user}">
	<c:choose>
		<c:when test="${signin.disabled && !hideEnableAccount}">
			<%-- we show this instead of forwarding to /account since you should still be able to see your 
				pages such as /home, /person with a disabled account --%>
			<div id="dhInformationBar"><a class="dh-information" href="/account">(re-enable your account)</a></div>
		</c:when>
		<c:when test="${browser.windows && !signin.user.account.wasSentShareLinkTutorial}">
			<div id="dhInformationBar"><a class="dh-information" href="/welcome">Download Mugshot Software Now</a></div>
		</c:when>
	</c:choose>
</c:if>
