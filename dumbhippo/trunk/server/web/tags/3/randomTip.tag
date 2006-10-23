<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="tipIndex" required="true" type="java.lang.Number" %>

<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="request"/>

<div class="dh-page-options-tip-area">
	<c:choose>
		<c:when test="${tipIndex == 0}">
			<a class="dh-page-options-tip-link" href="/invitation">Invite your friends to Mugshot!</a>
		</c:when>
		<c:when test="${tipIndex == 1}">
			<a class="dh-page-options-tip-link" href="/download">Download the Mugshot Client</a>
		</c:when>
		<c:when test="${tipIndex == 2 && browser.gecko}">
			<a class="dh-page-options-tip-link" href="/bookmark">Add Web Swarm link to Firefox</a>
		</c:when>		
	</c:choose>
</div>