<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="backgroundColor" required="false" type="java.lang.String" %>

<c:if test="${empty backgroundColor}">
	<c:set var="backgroundColor" value="grey1"/>
</c:if> 

<table class="dh-box-${backgroundColor} dh-download-section" cellspacing="0" cellpadding="0">
	<tr>
	<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/musicradar46x60.png" style="width: 46; height: 60;"/></td>
	<td class="dh-download-section-details-area"><div class="dh-download-section-header">Display <a class="dh-download-product" href="/radar-learnmore">Music Radar</a> on your MySpace, LiveJournal, or blog page.</div>
	<div class="dh-download-section-description">
	Show off your iTunes, Rhapsody, or Yahoo! Music Player playlist.  See what your friends are listening to and explore new music.
	When someone clicks on your Music Radar, they'll be taken to your Mugshot page to see more about you and your tastes.</div>
	<br/>
	<div><div class="dh-download-section-subheader">The Mugshot download lets you...</div>
		<ul class="dh-download-section-list">
			<li>Create and customize your own Music Radar to display on MySpace, LiveJournal, or other blog site</li>
			<li>Display your music playlists as you're listening</li>
		</ul>
		<jsp:doBody/>
	</td>
	</tr>
</table>