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
			<div class="dh-download-subtitle">Take advantage of all of our fun features with the Mugshot download.  It's easy and free!</div>
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
			<div class="dh-download-buttons">
				<%-- the class changes to dh-download-product-disabled in the javascript when one is not active --%>
				<a id="dhDownloadProduct" class="dh-download-product" href="javascript:dh.download.doDownload('${welcome.downloadUrl}')"><img src="/images3/${buildStamp}/download_now_button.gif"/></a>
				<a id="dhSkipDownload" class="dh-download-product" href="javascript:dh.download.doDownload()"><img src="/images3/${buildStamp}/no_thanks_button.gif"/></a>
			</div>
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
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><img src="/images3/${buildStamp}/stackericon59x56.png"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header"><a class="dh-download-product" href="/music-learnmore">Mugshot Stacker</a> gives you instant updates on your desktop.</div>
			<div class="dh-download-section-description">
			It's like a mini version of your Mugshot page, showing instant updates from Web Swarm, Music Radar, Mugshot Groups, and your
			other sites like MySpace and Facebook.</div>
			<br/>
			<div><div class="dh-download-section-subheader">The Mugshot download lets you...</div>
				<ul class="dh-download-section-list">
					<li>Have the functionality of your Mugshot page on a conveinent desktop application</li>
					<li>Get notified when you and your friends have updates at MySpace, Facebook, and other social network sites</li>
					<li>Decide when you want to be notified of new activity</li>
					<li>Scan recent online activity from your friends without visiting lots of web sites</li>
				</ul>
				<a class="dh-download-learnmore dh-underlined-link" href="/stacker-learnmore">Learn more</a>
			</td>
			</tr>
		</table>
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
					<div><dh:png src="/images3/${buildStamp}/windows_tooltray.png" style="width: 182px; height: 48px; padding: 5px 0px;"/></div>
					Mugshot icon in tool tray
				</c:when>
				<c:when test="${browser.linux}">
					<div>Look in your panel for the Mugshot icon - it will look something like this:</div>				
					<div><dh:png src="/images3/${buildStamp}/linux_tooltray.png" style="width: 156px; height: 44px; padding: 5px 0px;"/></div>
					Mugshot icon in tool tray							
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
