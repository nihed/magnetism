<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Stacker - Learn More</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="download"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="stacker-learnmore">
	<dht3:shinyBox color="grey">
		<div class="dh-download-header-area">
			<div class="dh-download-header">Mugshot Stacker</div>
			<dht3:learnMoreNextStep page="stacker"/>
		</div>
		<dht3:stackerLearnMore>
			<hr height="1px" color="#666666" style="margin: 10px 0px"/>
			<div class="dh-download-section-header">Using <span class="dh-download-product">Mugshot Stacker</span></div>
			<div class="dh-download-section-description">
				<p>After the <a class="dh-underlined-link" href="/download">Mugshot download</a> is installed, an icon
				will appear in your desktop tool tray.  Click it to activate Mugshot Stacker.</p>
				<p>
				<dht3:toolTrayPointer/>
				</p>
				<p>
				Mugshot Stacker displays new activity from your friends and groups as it occurs.  Get updates from <!-- your MySpace, LiveJournal,
				Facebook, and other sites--> the sites you use by adding them to your <a href="/account">Account</a>.
				</p>
				<p>
					<img src="/images3/${buildStamp}/stacker_browsewin_01.jpg"/>
				</p>
				<p>
					<div class="dh-download-section-subheader">Features</div>
					<ul class="dh-download-section-list">
						<li>Clicking the Home button takes you to your Mugshot Home page.</li>
						<li>Scroll down the Mugshot Stacker window to view a history of activity and see older posts.</li>
						<li>Expand the window by dragging the bottom bar.</li>
						<li>Click Hush to temporarily disable update alerts, and click Unhush to re-enable them.</li>
						<li>Hide Mugshot Stacker by clicking the X button.  Click the tool tray icon to reactivate it.</li>
					</ul>
				</p>
			</div>
		</dht3:stackerLearnMore>
				<dht3:learnMoreOptions exclude="stacker"/>
	</dht3:shinyBox>
</dht3:page>
