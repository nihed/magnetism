<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Music Radar - Learn More</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="download"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="radar-learnmore">
	<dht3:shinyBox color="grey">
		<div class="dh-download-header-area">
			<div class="dh-download-header">Music Radar</div>
			<dht3:learnMoreNextStep page="musicRadar"/>
		</div>
		<dht3:radarLearnMore>
			<hr height="1px" color="#666666" style="margin: 10px 0px"/>
			<div class="dh-download-section-header">Using <span class="dh-download-product">Music Radar</span></div>
			<div class="dh-download-section-description">
				<p>On your <a class="dh-underlined-link" href="/account">Account</a> page, go to the <b>Music Radar</b> section.  Make sure Music sharing is on.
				<div>Click the link to <a class="dh-underlined-link" href="radar-themes">Edit my Music Radar theme</a> to create or modify your Music Radar.</div></p>
				<p>
				<div>On that page, you can browse and edit the available themes, or create one from scratch.</div>
				<div>When your Music Radar theme is created, the page will give you HTML code and instructions to copy and paste it into your MySpace,
				LiveJournal, or blog page.  Once the code is added to your page, your Music Radar will look something like this:</div>
				</p>
				<p>
					<dht:beaconSamples/>
				<p>
				Visitors to your blog can see what you're listening to, and click the Music Radar to go to your Mugshot page.
				</p>
				<p>
					<div class="dh-download-section-header">If Music Radar Doesn't work on my blog:</div>
					<div>Go to your <a class="dh-underlined-link" href="/account">Account</a> page and make sure Music sharing is on.</div>
					<div>You must be running iTunes, Rhapsody, Yahoo! Music Engine, Rhythmbox, or another supported music player.</div>
				</p>
			</div>
		</dht3:radarLearnMore>
		<dht3:learnMoreOptions exclude="musicRadar"/>	
	</dht3:shinyBox>
</dht3:page>
