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
	<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/stackericon59x56.png" style="width: 59; height: 56;"/></td>
	<td class="dh-download-section-details-area"><div class="dh-download-section-header"><a class="dh-download-product" href="/stacker-learnmore">Mugshot Stacker</a> gives you instant updates on your desktop.</div>
	<div class="dh-download-section-description">
	It's like a mini version of your Mugshot page, showing instant updates from Web Swarm, Music Radar, Mugshot Groups, and your
	other sites<!-- like MySpace and Facebook-->.</div>
	<br/>
	<div><div class="dh-download-section-subheader">The Mugshot download lets you...</div>
		<ul class="dh-download-section-list">
			<li>Have the functionality of your Mugshot page on a convenient desktop application</li>
			<!-- <li>Get notified when you and your friends have updates at MySpace, Facebook, and other social network sites</li> -->
			<li>Decide when you want to be notified of new activity</li>
			<li>Scan recent online activity from your friends without visiting lots of web sites</li>
		</ul>
		<jsp:doBody/>				
	</td>
	</tr>
</table>
