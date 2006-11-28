<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="bookmark" class="com.dumbhippo.web.pages.BookmarkPage" scope="request"/>

<head>
	<title>Mugshot Web Swarm - Learn More</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="download"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>

<c:choose>
	<c:when test='${browser.geckoRequested}'>
		<c:set var="dragDestImg" value="bookmarkfirefox.gif" scope="page"/>
		<c:set var="browserTitle" value="FIREFOX" scope="page"/>
		<c:set var="unsupported" value="false" scope="page"/>	
	</c:when>
	<c:when test='${browser.safariRequested}'>
		<c:set var="dragDestImg" value="bookmarksafari.gif" scope="page"/>
		<c:set var="browserTitle" value="SAFARI" scope="page"/>
		<c:set var="unsupported" value="false" scope="page"/>	
	</c:when>
</c:choose>

<dht3:page>
	<dht3:shinyBox color="grey">
		<div class="dh-download-header-area">
			<div class="dh-download-header">Web Swarm</div>
			<dht3:learnMoreNextStep page="webSwarm"/>
		</div>
		<dht3:webSwarmLearnMore>
			<hr height="1px" color="#666666" style="margin: 10px 0px"/>
			<div class="dh-download-section-header">Using <span class="dh-download-product">Web Swarm</span></div>
			<div class="dh-download-section-description">
			<p>
			<c:choose>
				<c:when test="${browser.ieRequested}">
					<div>Since you're using Internet Explorer, you can click the Web Swarm button on your browser's toolbar.</div>
					<img src="/images2/${buildStamp}/toolbarsample.gif"/>
				</c:when>
				<c:when test='${browser.geckoRequested || browser.safariRequested}'>
					<div>Drag the link below onto the Bookmarks toolbar.  Then to share and chat about
						a site with friends, click 'Mugshot Web Swarm' on your Bookmarks toolbar.</div>
					<div id="dhBookmarkHowto">
					<table style="background-color:#ffffff;color:inherit;" cellspacing="10px" cellpadding="0">
					<tr>
					<td width="10px;"><div></div></td>
					<td align="left" valign="bottom"><img src="/images2/${buildStamp}/dragthis.gif"/></td>
					<td align="center" valign="bottom">
					<div id="dhBookmarkLink">
					<a href="javascript:window.open('${bookmark.baseUrl}/sharelink?v=1&url='+encodeURIComponent(location.href)+'&title='+encodeURIComponent(document.title)+'&next=close','_NEW','menubar=no,location=no,toolbar=no,scrollbars=yes,status=no,resizable=yes,height=400,width=550,top='+((screen.availHeight-400)/2)+',left='+((screen.availWidth-550)/2));void(0);">Mugshot Web Swarm</a>
					</div>
					</td>
					<td align="right" valign="bottom"><img src="/images2/${dragDestImg}"/></td>
					<td width="10px;"><div></div></td>	
					</tr>
					</table>
					</div>				
				</c:when>
			</c:choose>
			<div>
			Instructions for: <a class="dh-option-list-option" href="?browser=ie">Internet Explorer</a> | 
			<a class="dh-option-list-option" href="?browser=gecko">Firefox</a> | 
			<a class="dh-option-list-option" href="?browser=safari">Safari</a>						
			</div>
			</p>
			<p>To share a web page you're visiting,   In the Web Swarm window,
			enter Mugshot Groups, users or friends' e-mails as recipients.  Write a quick description, and send it off.
			</p>
			<p>A new entry in your stack will appear when you receive Web Swarm shares from friends or Groups.  Click the link in your
			stack to visit the page.  In the Web Swarm bar at the bottom of the shared page, you can see who sent it, who else is there,
			and recent chat comments.  There are also links to forward the page to others, and to save the page as one of your Faves.
			</p>		
			</div>
		</dht3:webSwarmLearnMore>
		<dht3:learnMoreOptions exclude="webSwarm"/>
	</dht3:shinyBox>
</dht3:page>
