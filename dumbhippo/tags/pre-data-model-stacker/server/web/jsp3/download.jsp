<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="download" class="com.dumbhippo.web.DownloadBean" scope="page"/>

<head>
	<title>Mugshot Download</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="download"/>
	<dh:script modules="dh.event"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="download">
	<dht3:accountStatus includeDownload="false"/>
	<dht3:shinyBox color="grey">
		<div class="dh-download-header-area">
			<div class="dh-download-header">Download Mugshot</div>
			<dht3:download page="download"/>
			<div class="dh-download-subheader">Here's what you can do with the Mugshot download...</div>
		</div>
		<dht3:stackerLearnMore>
			<a class="dh-download-learnmore dh-underlined-link" href="/stacker-learnmore">Learn more</a>
		</dht3:stackerLearnMore>		
		<dht3:radarLearnMore>
			<a class="dh-download-learnmore dh-underlined-link" href="/radar-learnmore">Learn more</a>	
		</dht3:radarLearnMore>
		<dht3:webSwarmLearnMore>
			<a class="dh-download-learnmore dh-underlined-link" href="/links-learnmore">Learn more</a>		
		</dht3:webSwarmLearnMore>		
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
				<c:when test="${download.windows}">
					<div>Look in your Windows system tray for the Mugshot icon - it will look something like this:</div>
					<dht3:toolTrayPointer/>
				</c:when>
				<c:when test="${download.linux}">
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
