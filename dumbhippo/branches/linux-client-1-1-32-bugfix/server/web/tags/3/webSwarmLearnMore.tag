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
	<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/webswarm63x50.png" style="width: 63; height: 50;"/></td>
	<td class="dh-download-section-details-area"><div class="dh-download-section-header">Use <a class="dh-download-product" href="/links-learnmore">Web Swarm</a> to share and chat about cool links with friends.</div>
	<div class="dh-download-section-description">
	Stay up to date on web trends, and save your favorite sites.  You and friends can share what you think is interesting online.  See
	how much interest sites are getting in real time.  Nothing to clog up your email inbox, and no worries about who's using what kind of IM.
	</div>
	<br/>
	<div><div class="dh-download-section-subheader">The Mugshot download lets you...</div>
		<ul class="dh-download-section-list">
			<li>Share web pages with lists of friends instantly</li>
			<li>Get notifications when friends share new content or when RSS feeds are updated</li>
			<li>See who is checking out the pages you share, and chat with them</li>
			<li>Join Groups that suit your interests, and get notified about new activity</li>
		</ul>
		<jsp:doBody/>		
	</td>
	</tr>
</table>