<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.NowPlayingThemesPage" scope="request"/>

<head>
        <title>Music Radar Themes</title>
	<link rel="stylesheet" type="text/css" href="/css2/radar.css"/>        
	<dht:faviconIncludes/>
        <dht:scriptIncludes/>
        <script type="text/javascript">
	        dojo.require("dh.nowplaying");
		</script>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxMusic>
		
		<c:if test="${!empty nowplaying.currentTheme}">		
			<dht:zoneBoxTitle>CURRENT THEME</dht:zoneBoxTitle>	
			<dht:radarTheme theme="${nowplaying.currentTheme}" signin="${nowplaying.signin}" alreadyCurrent="true"/>		
			<dht:zoneBoxSeparator/>		
		</c:if>

		<c:if test="${nowplaying.myThemes.size > 0}">
			<dht:zoneBoxTitle>MY CREATIONS</dht:zoneBoxTitle>	
			<div>
				<c:forEach items="${nowplaying.myThemes.list}" var="theme" varStatus="status">
					<dht:radarTheme theme="${theme}" signin="${nowplaying.signin}"/>
				</c:forEach>
			</div>
			<dht:zoneBoxSeparator/>			
		</c:if>	
		
		<c:if test="${nowplaying.randomThemes.size > 0}">
			<dht:zoneBoxTitle>MORE THEMES</dht:zoneBoxTitle>
			
			<div>
				<c:forEach items="${nowplaying.randomThemes.list}" var="theme" varStatus="status">
					<dht:radarTheme theme="${theme}" signin="${nowplaying.signin}"/>
				</c:forEach>		
			</div>
		</c:if>		
			
		</dht:zoneBoxMusic>
	</dht:contentColumn>	
</dht:twoColumnPage>
</html>
