<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="main"/>
	<dht:faviconIncludes/>
    <dh:script modules="dh.main,dh.event"/>
	<script type="text/javascript">
		dh.event.addPageLoadListener(dhMainInit);
	</script>
</head>

<dh:bean id="main" class="com.dumbhippo.web.pages.MainPage" scope="request"/>

<dht3:page currentPageLink="main">
		<dht3:shinyBox color="purple">
		    <div id="dhMugshotWelcome">
		    <div id="dhMugshotIntro"> 
		        <div class="dh-intro-message">Put your Mugshot on the Web.</div>
		        <div class="dh-intro-explanation">Show updates from all your sites on one page. Get live updates from friends.  Mugshot makes it fun, free and easy!</div>
			<div class="dh-intro-text">Share content from <em>MySpace</em>, <em>YouTube</em>, <em>Facebook</em>, <em>Flickr</em>, blogs and more. Why go to a ton of different sites every day when you can check out just one?</div>
		        <div class="dh-intro-options">
			<span class="dh-button"><a href="/who-are-you"><img src="/images3/${buildStamp}/login.gif"/></a></span>
			&nbsp; or &nbsp;
			<span class="dh-button"><a href="/features"><img src="/images3/${buildStamp}/learnmore.gif"/></a></span>                
                </div>
            </div>
            <div id="dhMugshotExample">
                <img src="/images3/${buildStamp}/sampleMugshot.gif" style="width: 620px; height: 346px; border: none;"/>
            </div>     
            </div>        
		</dht3:shinyBox>
		<div class="dh-main-people dh-half-shinybox-left-container">
            <div class="dh-header"><a href="/active-people">Active People</a></div>
            <c:forEach items="${main.recentUserActivity.list}" var="personMugshot" varStatus="status">
                <dht3:personStack person="${personMugshot.personView}" stackOrder="${status.count}" blocks="${personMugshot.blocks}" showFrom="false" embedVersion="true"/>
            </c:forEach>
        </div>
        <div class="dh-main-groups dh-half-shinybox-right-container">            
            <div class="dh-header"><a href="/active-groups">Active Groups</a></div>
            <c:forEach items="${main.recentGroupActivity.list}" var="groupMugshot" varStatus="status">
		        <dht3:groupStack who="${groupMugshot.groupView}" stackOrder="${status.count + main.recentUserActivity.size}" blocks="${groupMugshot.blocks}" showFrom="false" embedVersion="true"/>
           </c:forEach>
       </div>    
</dht3:page>
