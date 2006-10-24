<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="welcome" class="com.dumbhippo.web.pages.DownloadPage" scope="page"/>
<jsp:setProperty name="welcome" property="invitationId" param="invite"/>
<jsp:setProperty name="welcome" property="inviterId" param="inviter"/>

<c:set var="urlParams" value=''/>
<c:set var="acceptMessage" value='false'/>
<c:if test='${!empty param["acceptMessage"]}'>
    <c:set var="acceptMessage" value='${param["acceptMessage"]}'/>
    <c:set var="urlParams" value='&acceptMessage=${param["acceptMessage"]}'/>    
</c:if>
<c:if test='${!empty param["invite"]}'>
    <c:set var="urlParams" value='${urlParams}&invite=${param["invite"]}'/>    
</c:if>
<c:if test='${!empty param["inviter"]}'>
    <c:set var="urlParams" value='${urlParams}&inviter=${param["inviter"]}'/>    
</c:if>

<head>
	<title>Mugshot Download</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="download"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
	<script type="text/javascript">
		dojo.require("dh.download");
		dh.download.needTermsOfUse = ${signin.needsTermsOfUse}
		dojo.event.connect(dojo, "loaded", function () { dh.download.init() })
	</script>	
</head>

<dht3:page>
	<dht3:shinyBox color="grey">
		<div class="dh-download-header-area">
			<div class="dh-download-header">Get Mugshot</div>
			<c:if test="${welcome.haveDownload}">
				<div class="dh-download-subtitle">Take advantage of all of our fun features with the Mugshot download.  It's easy and free!</div>
			</c:if>
			<div class="dh-download-terms">
				<c:choose>
					<c:when test="${signin.needsTermsOfUse}">
			        	<div id="dhAcceptTermsBox">        
					        <input type="checkbox" id="dhAcceptTerms" onclick="dh.download.updateDownload();">
						                I accept the Mugshot <a href="javascript:window.open('/terms', 'dhTermsOfUse', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">terms and conditions</a>. (Required to continue.)
					        </input>
						</div>
		            </c:when>    
		            <c:otherwise>
		            </c:otherwise>
		        </c:choose>
			</div>
			<c:choose>
				<c:when test="${welcome.haveDownload}">
					<div class="dh-download-buttons">
						<%-- the class changes to dh-download-product-disabled in the javascript when one is not active --%>
						<a id="dhDownloadProduct" class="dh-download-product" href="javascript:dh.download.doDownload('${welcome.downloadUrl}')"><img src="/images3/${buildStamp}/download_now_button.gif"/></a>
						<a id="dhSkipDownload" class="dh-download-product" href="javascript:dh.download.doDownload()"><img src="/images3/${buildStamp}/no_thanks_button.gif"/></a>
						<c:if test="${browser.linuxRequested}">
							<i>  (This download is for <c:out value="${welcome.downloadFor}"/>.)</i>
						</c:if>
					</div>					
					<div class="dh-download-yadayada">
						<c:choose>
							<c:when test="${browser.linuxRequested}">
								Or, get Mugshot for <a href="/download?platform=windows${urlParams}">Windows</a> instead.
							</c:when>
							<c:otherwise>
								Or, get Mugshot for <a href="/download?distribution=fedora5${urlParams}">Fedora Core 5</a> instead.
							</c:otherwise>
						</c:choose>
					</div>
				</c:when>
				<c:otherwise>
					<div class="dh-download-yadayada">
						We don't have Mugshot for your computer yet. (You can 
						download for <a href="/download?platform=windows${urlParams}">Windows</a> or 
						<a href="/download?distribution=fedora5${urlParams}">Fedora Core 5</a>.)
						<c:if test="${browser.macRequested}">We're still working on Mac OS X support.</c:if>
						You can use Mugshot without the download, however.
					</div>
					<div class="dh-download-yadayada">
						<a id="dhSkipDownload" class="dh-download-product" href="javascript:dh.download.doDownload()">Continue without downloading.</a>
					</div>
					<c:if test="${browser.linuxRequested}">
						<div class="dh-download-yadayada">						
							Contributed third-party builds <a href="http://developer.mugshot.org/wiki/Downloads">can be found on the Mugshot Wiki</a>.
							<c:if test="${signin.needsTermsOfUse}">Please also click "Continue without downloading"
							above to accept the terms of use, or Mugshot won't work.</c:if>
						</div>
					</c:if>
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${browser.linuxRequested}">
					<div class="dh-download-yadayada">
						<i>Source code is available in <a href="${welcome.downloadUrlLinuxTar}">tar.gz</a> and
						<a href="${welcome.downloadUrlLinuxSrpm}">SRPM</a> formats.</i>
					</div>
				</c:when>
			</c:choose>			
			<div class="dh-download-subheader">Here's what you can do with the Mugshot download...</div>
		</div>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><img src="/images3/${buildStamp}/webswarm63x50.png"/></td>
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
				<a class="dh-download-learnmore dh-underlined-link" href="/links-learnmore">Learn more</a>				
			</td>
			</tr>
		</table>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><img src="/images3/${buildStamp}/musicradar46x60.png"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Display <a class="dh-download-product" href="/music-learnmore">Music Radar</a> on your MySpace, LiveJournal, or blog page.</div>
			<div class="dh-download-section-description">
			Show off your iTunes, Rhapsody, or Yahoo! Music Player playlist.  See what your friends are listening to and explore new music.
			When someone clicks on your Music Radar, they'll be taken to your Mugshot page to see more about you and your tastes.</div>
			<br/>
			<div><div class="dh-download-section-subheader">The Mugshot download lets you...</div>
				<ul class="dh-download-section-list">
					<li>Create and customize your own Music Radar to display on MySpace, LiveJournal, or other blog site</li>
					<li>Display your music playlists as you're listening</li>
				</ul>
				<a class="dh-download-learnmore dh-underlined-link" href="/music-learnmore">Learn more</a>				
			</td>
			</tr>
		</table>
		<dht3:stackerLearnMore>
			<a class="dh-download-learnmore dh-underlined-link" href="/stacker-learnmore">Learn more</a>
		</dht3:stackerLearnMore>
		<div class="dh-download-faqs-header">Mugshot Download FAQs</div>
		<div class="dh-download-faq">
			<div class="dh-download-faq-q">Why do I need to download anything to use Mugshot?</div>
			<div class="dh-download-faq-a">The Mugshot download isn't required, but is recommended.  Your Mugshot page will work without it, but with it you'll have a
			more robust experience.</div>
		</div>
		<div class="dh-download-faq">
			<div class="dh-download-faq-q">How do I know Mugshot is running on my PC?</div>
			<div class="dh-download-faq-a">
			<c:choose>
				<c:when test="${browser.windows}">
					<div>Look in your Windows system tray for the Mugshot icon - it will look something like this:</div>
					<dht3:toolTrayPointer/>
				</c:when>
				<c:when test="${browser.linux}">
					<div>Look in your panel for the Mugshot icon - it will look something like this:</div>				
					<dht3:toolTrayPointer/>						
				</c:when>
				<c:otherwise>
					No instructions available right now for your computer operating system - check back later!
				</c:otherwise>
			</c:choose>
			</div>
		</div>
		<div class="dh-download-faq">
			<div class="dh-download-faq-q">Do I need a Mugshot account to use it?</div>
			<div class="dh-download-faq-a">Without an account, the Mugshot download doesn't do anything useful except sit in your system tray and look pretty.  <a href="/signup">Sign up!</a></div>
		</div>
		<div class="dh-download-faq">
			<div class="dh-download-faq-q">Is there a Mugshot download for Mac?</div>
			<div class="dh-download-faq-a">Not right now, but it's on our "to do" list.  If you're a developer, you can <a href="http://developer.mugshot.org/">help develop</a> it!</div>
		</div>						
	</dht3:shinyBox>
</dht3:page>
