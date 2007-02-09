<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="isSelf" required="true" type="java.lang.Boolean" %>

<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="request"/>

<c:set var="tipIndex" value="${dh:randomInt(13)}" scope="page"/>

<div class="dh-page-options-tip-area">
	<c:choose>
	    <c:when test="${!isSelf || accountStatusShowing}">
		    &nbsp;	    
	    </c:when>
		<c:when test="${tipIndex == 0}">
			<a class="dh-page-options-tip-link" href="/invitation">Invite your friends to Mugshot!</a>
		</c:when>
		<c:when test="${tipIndex == 1}">
			<a class="dh-page-options-tip-link" href="/download">Get the Mugshot download</a>
		</c:when>
		<c:when test="${tipIndex == 2 && browser.gecko}">
			<a class="dh-page-options-tip-link" href="/bookmark">Add Web Swarm link to Firefox</a>
		</c:when>		
		<c:when test="${tipIndex == 3}">
			<a class="dh-page-options-tip-link" href="/badges">Put Mini Mugshot on your MySpace</a>
		</c:when>
		<c:when test="${tipIndex == 4}">
			<a class="dh-page-options-tip-link" href="/badges">Add Mini Mugshot to your Blog sidebar</a>
		</c:when>
		<c:when test="${tipIndex == 5}">
			<a class="dh-page-options-tip-link" href="/account">Last.fm geek?  We've got you covered</a>
		</c:when>
		<c:when test="${tipIndex == 6}">
			<a class="dh-page-options-tip-link" href="/account">Facebook live updates</a>
		</c:when>
		<c:when test="${tipIndex == 7}">
			<a class="dh-page-options-tip-link" href="/account">MySpace blogger?  Get live updates</a>
		</c:when>
		<c:when test="${tipIndex == 8}">
			<a class="dh-page-options-tip-link" href="/account">Flickr live photo updates</a>
		</c:when>
		<c:when test="${tipIndex == 9}">
			<a class="dh-page-options-tip-link" href="/account">YouTube video upload updates</a>
		</c:when>
		<c:when test="${tipIndex == 10}">
			<a class="dh-page-options-tip-link" href="/account">Blog much?  Add it here</a>
		</c:when>
		<c:when test="${tipIndex == 11}">
			<a class="dh-page-options-tip-link" href="/account">Rhapsody playlist in Music Radar</a>
		</c:when>
		<c:when test="${tipIndex == 12}">
			<a class="dh-page-options-tip-link" href="/buttons">Spread Mugshot! Add a button to your blog.</a>
		</c:when>		
		<c:otherwise>
		    &nbsp;
		</c:otherwise>
	</c:choose>
</div>
