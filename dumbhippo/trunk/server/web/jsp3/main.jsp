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
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
	<script type="text/javascript" src="/javascript/${buildStamp}/dh/main.js"></script>
	<script type="text/javascript">
		dojo.require("dh.main")
		dojo.event.connect(dojo, "loaded", dj_global, "dhMainInit");
	</script>
</head>

<dh:bean id="main" class="com.dumbhippo.web.pages.MainPage" scope="request"/>

<dht3:page>
		<dht3:shinyBox color="purple">
		    <div id="dhMugshotIntro">
		        <div class="dh-intro-message">Put you mugshot on the web.</div>
		        <div class="dh-intro-explanation">Everything you and your friends do on the web, in real time.</div>
		        <div class="dh-intro-options">
                    <span class="dh-button"><a href="/signup"><img src="/images3/${buildStamp}/signup.gif"/></a></span>
                    &nbsp; or &nbsp;
                    <span class="dh-button"><a href="/who-are-you"><img src="/images3/${buildStamp}/login.gif"/></a></span>
               </div> 
           </div>        
		</dht3:shinyBox>
		<div class="dh-main-people">
            <div class="dh-header">Active People</div>
            <c:forEach items="${main.recentUserActivity.list}" var="personMugshot" varStatus="status">
                <dht3:personStack contact="${personMugshot.personView}" stackOrder="${status.count}" blocks="${personMugshot.blocks}" showFrom="false" embedVersion="true"/>
            </c:forEach>
        </div>
        <div class="dh-main-groups">            
            <div class="dh-header">Active Groups</div>
            <c:forEach items="${main.recentGroupActivity.list}" var="groupMugshot" varStatus="status">
		        <dht3:groupStack who="${groupMugshot.groupView}" stackOrder="${status.count + main.recentUserActivity.size}" blocks="${groupMugshot.blocks}" showFrom="false" embedVersion="true"/>
           </c:forEach>
       </div>    
</dht3:page>
