<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="separator" required="false" type="java.lang.Boolean" %>
<%@ attribute name="musicLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="browseLink" required="false" type="java.lang.Boolean" %>

<%-- edit / browse and music links will appear by default and have to be turned off explicitly --%>

<table><tr>
<td rowSpan="2" valign="top"><img src="/images2/${buildStamp}/musicradar32x44.gif" /></td>
<td class="dh-promo-description"><strong>Music Radar</strong> shares your music taste with friends and on your blog.</td>
</tr><tr>
<td>
	<span class="dh-option-list">
	<%-- Just want to hack the style of the first element --%>
	<a style="margin-left:0;" class="dh-option-list-option" href="/radar-learnmore">Learn more</a>
<c:if test="${empty browseLink || browseLink}">
	| 
	<c:choose>
		<c:when test="${signin.valid}">
			<a class="dh-option-list-option" href="/radar-themes">Edit and browse themes</a>
		</c:when>
		<c:otherwise>
			<a class="dh-option-list-option" href="/radar-themes">Browse themes</a>
		</c:otherwise>
	</c:choose>
</c:if>
	</span>
</td>
</tr></table>
<c:if test="${!empty separator && separator}">
	<dht:zoneBoxSeparator/>
</c:if>

