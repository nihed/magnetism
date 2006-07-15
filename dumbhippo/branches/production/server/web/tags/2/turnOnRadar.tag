<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.musicSharingEnabled}">
	<table><tr>
	<td rowSpan="2"><img src="/images2/${buildStamp}/musicradar32x44.gif" /></td>
	<td><a style="font-weight:bold;" href="javascript:dh.actions.setMusicSharingEnabled(true);">Turn on Music Radar</a> to share your music taste with others.</td>
	</tr><tr>
	<td>
	<span class="dh-option-list"><%-- Just want to hack the style of this one element --%>
	<a style="margin-left:0;" class="dh-option-list-option" href="/radar-learnmore">Learn more</a>
	|
	<a class="dh-option-list-option" href="/radar-themes">Browse themes</a>
	</span>
	</td>
	</tr></table>
	<dht:zoneBoxSeparator/>
</c:if>
