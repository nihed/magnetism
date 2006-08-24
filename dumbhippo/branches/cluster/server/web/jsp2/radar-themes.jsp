<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.pages.NowPlayingThemesPage" scope="request"/>

<head>
        <title>Music Radar Themes</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/radar.css"/>        
	<dht:faviconIncludes/>
        <dht:scriptIncludes/>
        <script type="text/javascript">
	        dojo.require("dh.nowplaying");
		</script>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxMusic>
		<dht:musicRadarPromo browseLink="false" separator="true"/>		
		<c:if test="${nowplaying.signin.valid}">
		<c:if test="${!empty nowplaying.currentTheme}">		
			<dht:zoneBoxTitle>CURRENT THEME</dht:zoneBoxTitle>	
			<dht:radarTheme theme="${nowplaying.currentTheme}" signin="${nowplaying.signin}" userId="${nowplaying.radarCharacterId}" alreadyCurrent="true"/>		
			<dht:zoneBoxSeparator/>		
		</c:if>

		<c:if test="${nowplaying.myThemes.count > 0}">
			<dht:zoneBoxTitle a="dhMyThemes">YOUR CREATIONS</dht:zoneBoxTitle>	
			<div>
				<c:forEach items="${nowplaying.myThemes.results}" var="theme" varStatus="status">
					<dht:radarTheme theme="${theme}" signin="${nowplaying.signin}" userId="${nowplaying.radarCharacterId}"/>
				</c:forEach>
			</div>
			<dht:expandablePager pageable="${nowplaying.myThemes}" anchor="dhMyThemes"/>			
			<dht:zoneBoxSeparator/>			
		</c:if>
		</c:if>
		
		<c:if test="${nowplaying.randomThemes.count > 0}">
			<dht:zoneBoxTitle a="dhAllThemes">MORE THEMES</dht:zoneBoxTitle>
			
			<div>
				<c:forEach items="${nowplaying.randomThemes.results}" var="theme" varStatus="status">
					<dht:radarTheme theme="${theme}" signin="${nowplaying.signin}" userId="${nowplaying.radarCharacterId}"/>
				</c:forEach>		
			</div>
			<dht:expandablePager pageable="${nowplaying.randomThemes}" anchor="dhAllThemes"/>				
		</c:if>		
			
		</dht:zoneBoxMusic>
	</dht:contentColumn>	
</dht:twoColumnPage>
</html>
