<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Features</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="download"/>	
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="features">
	<dht3:shinyBox color="grey">
	    <div class="dh-page-shinybox-title-large">Mugshot Features</div>
		<div class="dh-download-header-area">
			<dht3:learnMoreNextStep/>	
		</div>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/mugshot70x64.png" style="width: 70; height: 64;"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Set up your own <span class="dh-download-product">Mugshot</span>.</div>
			<div class="dh-download-section-description">
				It's an easy way for you and friends to keep track of one other online.  Show a list of sites you belong to, like MySpace, <!--Digg,--> and LiveJournal.  Display
				photo streams from your Flickr account.  Share what you're browsing and listening to, using Web Swarm and Music Radar.  Your Mugshot page also
				includes friends' recent web activity.  Keep up with everything on one convenient page!
			    <div>
			    <a class="dh-download-learnmore dh-underlined-link" href="/web-accounts-learnmore">Learn more</a>
			    &nbsp;or&nbsp;	
				<a class="dh-underlined-link" href="/active-people">See some members' Mugshots</a></div>
				</div>
			</div>
			</td>
			</tr>
		</table>		
		<dht3:webSwarmLearnMore backgroundColor="grey2">
			<a class="dh-download-learnmore dh-underlined-link" href="/links-learnmore">Learn more</a>		
		</dht3:webSwarmLearnMore>
		<dht3:radarLearnMore backgroundColor="grey1">
			<a class="dh-download-learnmore dh-underlined-link" href="/radar-learnmore">Learn more</a>	
		</dht3:radarLearnMore>
		<dht3:stackerLearnMore backgroundColor="grey2">
			<a class="dh-download-learnmore dh-underlined-link" href="/stacker-learnmore">Learn more</a>
		</dht3:stackerLearnMore>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/groups70x59.png" style="width: 70; height: 59;"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Join some <span class="dh-download-product">Groups</span>.</div>
			<div class="dh-download-section-description">
				<p>Join groups for what interests you to share posts and RSS feeds.  Or <a href="/create-group">start your own</a>!</p>
				<p><span class="dh-download-section-subheader">Browse Groups by:</span> <a href="/active-groups">Activity</a></p>
			</div>
			</td>
			</tr>
		</table>
		<table class="dh-box-grey2 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/invite60x51.png" style="width: 60; height: 51;"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header"><span class="dh-download-product">Invite friends</span> to Mugshot.</div>
			<div class="dh-download-section-description">
				<div>It's free, fun and easy to use.  The more the merrier!</div>
				<div><a class="dh-underlined-link" href="/invitation">Invite friends</a></div>
			</div>
			</td>
			</tr>
		</table>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/minimugshot_face42x79.png" style="width: 42; height: 79;"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Put Mini Mugshot on your site.</div>
			<div class="dh-download-section-description">
				<div>Got lots of web accounts? Show where you're at and what you're up to at a glance. Add Mini Mugshot to blogs, Facebook, MySpace and more.</div>
				<div><a class="dh-underlined-link" href="/badges">See a live example</a></div>
			</div>
			</td>
			</tr>
		</table>		
		<table class="dh-box-grey2 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/mugshotluv_60x53.png" style="width: 60; height: 53;"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Spread the Mugshot Love.</div>
			<div class="dh-download-section-description">
				<div>Can't get enough Mugshot? Tell the world and build your network. (And ours!)</div>
				<div><a class="dh-underlined-link" href="/buttons">Add our buttons to your site or blog</a>, linking to your Mugshot page.</div>
			</div>
			</td>
			</tr>
		</table>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><dh:png src="/images3/${buildStamp}/underthehood54x65.png" style="width: 54; height: 65;"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Get under the hood.</div>
			<div class="dh-download-section-description">
				<div>Mugshot is all open source, so you can chip in on the design and development if that's your style.</div>
				<div><a class="dh-underlined-link" href="http://developer.mugshot.org">Learn more</a></div>
			</div>
			</td>
			</tr>
		</table>				
	</dht3:shinyBox>
</dht3:page>
