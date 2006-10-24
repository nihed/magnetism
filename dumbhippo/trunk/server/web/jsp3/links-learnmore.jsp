<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Web Swarm - Learn More</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="download"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>

<dht3:page>
	<dht3:shinyBox color="grey">
		<div class="dh-download-header-area">
			<div class="dh-download-header">Web Swarm</div>
			<div class="dh-download-subtitle">Take advantage of all of our fun features with the Mugshot <a class="dh-underlined-link" href="/download">download</a>.  It's easy and free!</div>
		</div>
		<dht3:webSwarmLearnMore>
			<hr height="1px" color="#666666" style="margin: 10px 0px"/>
			<div class="dh-download-section-header">Using <span class="dh-download-product">Web Swarm</span></div>
			<div class="dh-download-section-description">
			<p><div><img src="/images2/${buildStamp}/toolbarsample.gif"/></div>
			<div>To share a web page you're visiting, click the Web Swarm button on your browser's toolbar.  In the Web Swarm window,
			enter Mugshot Groups, users or friends' e-mails as recipients.  Write a quick description, and send it off.</div>
			</p>
			<p>A new entry in your stack will appear when you receive Web Swarm shares from friends or Groups.  Click the link in your
			stack to visit the page.  In the Web Swarm bar at the bottom of the shared page, you can see who sent it, who else is there,
			and recent chat comments.  There are also links to forward the page to others, and to save the page as one of your Faves.
			</p>		
			</div>
		</dht3:webSwarmLearnMore>
		<div>Learn more about: <a class="dh-underlined-link" href="links-learnmore">Web Swarm</a> | <a class="dh-underlined-link" href="stacker-learnmore">Mugshot Stacker</a></div>
	</dht3:shinyBox>
</dht3:page>
