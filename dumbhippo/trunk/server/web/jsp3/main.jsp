<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Home</title>
	<link rel="stylesheet" type="text/css" href="/css3/${buildStamp}/main.css"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>
<body class="dh-gray-background-page dh-home-page">
	<div id="dhPage">
		<dht3:header/>
		<dht3:shinyBox color="purple">
		    <table id="dhMugshotIntro">
		        <tr id="dhMugshotIntroHeader">
		        <td width="50%">What the Heck is Mugshot?</td>
		        <td width="50%">Mugshot Features</td>  
		        </tr> 
		        <tr valign="top">
	 	        <td>
	 	            <span class="dh-leading-text">A place to see what you and your friends are up to online.</span>
	 	            Keeping track of everybody's MySpace, Digg, Flickr, LiveJournal, and other sites is a lot of work.
	 	            Mugshot lets you see friends' updates at a glance. Instantly share interesting things at you social
	 	            network sites or with Mugshot's own Web Swarm tool. Receive feeds from groups that share your 
	 	            specific interests. Sign up for a free Mugshot account to make the Web fun again, and less like 
	 	            work!	 	        
	 	        </td>
	 	        <td>
	 	            <div><span class="dh-leading-text"><a href="http://blog.mugshot.org">The Stacker</a></span> is a desktop tool for instant updates from friends' sites.</div>
	 	            <div><span class="dh-leading-text"><a href="/links-learnmore">Web Swarm</a></span> lets you share and comment on cool sites.</div>
	 	            <div><span class="dh-leading-text"><a href="/radar-learnmore">Music Radar</a></span> shows what songs you and friends are listening to.</div>
	 	            <div><span class="dh-leading-text"><a href="/public-groups">Groups</a></span> can be created and joined for whatever you are interested in.</div>
	 	            <div><span class="dh-leading-text"><a href="http://blog.mugshot.org">Flickr and YouTube feeds</a></span> can be displayed on your Mugshot.</div>
	 	            <div><span class="dh-leading-text"><a href="http://blog.mugshot.org">Account icons</a></span> let friends know where you can be found online.</div>
	 	        </td>
	 	        </tr> 
	 	        <tr>
	 	        <td colspan="2">
	 	            <span class="dh-button"><img src="/images3/${buildStamp}/signup.gif"/></span>&nbsp; or &nbsp;<span class="dh-button"><img src="/images3/${buildStamp}/login.gif"/></span>
	 	        </td>
		    </table>
		</dht3:shinyBox>
		<dht:footer/>
	</div>
</body>
</html>
