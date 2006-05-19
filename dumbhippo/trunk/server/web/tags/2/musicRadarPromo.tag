<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="separator" required="false" type="java.lang.Boolean" %>
<%@ attribute name="musicLink" required="false" type="java.lang.Boolean" %>

<table><tr>
<td rowSpan="2"><img src="/images2/musicradar32x44.gif" /></td>
<td><strong>Music Radar</strong> shares your music taste with friends and on your blog.</td>
</tr><tr>
<td>
	<span class="dh-option-list"><%-- Just want to hack the style of this one element --%>
	<a style="margin-left:0;" class="dh-option-list-option" href="/radar-learnmore">Learn More</a>
	|
	<a class="dh-option-list-option" href="/radar-themes">Browse themes</a>
<c:if test="${!empty musicLink && musicLink}">
	|
	<a class="dh-option-list-option" href="/music">See what people are listening to</a>
</c:if>
	</span>
</td>
</tr></table>
<c:if test="${!empty separator && separator}">
	<dht:zoneBoxSeparator/>
</c:if>

