<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.NowPlayingThemesPage" scope="request"/>

<head>
        <title>Now Playing Themes</title>
        <dht:stylesheets />
        <dht:scriptIncludes/>
        <script type="text/javascript">
	        dojo.require("dh.nowplaying");
		</script>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/>

		<c:if test="${!empty nowplaying.currentTheme}">
			<dht:largeTitle>Your Current Theme</dht:largeTitle>
			
			<div>
				<dht:nowPlayingTheme theme="${nowplaying.currentTheme}" signin="${nowplaying.signin}" alreadyCurrent="true"/>
			</div>
		</c:if>
	
		<c:if test="${nowplaying.myThemes.size > 0}">
			<dht:largeTitle>My Creations</dht:largeTitle>
			
			<div>
				<c:forEach items="${nowplaying.myThemes.list}" var="theme" varStatus="status">
					<dht:nowPlayingTheme theme="${theme}" signin="${nowplaying.signin}"/>
				</c:forEach>
			</div>
		</c:if>
		
		<c:if test="${nowplaying.friendsThemes.size > 0}">
			<dht:largeTitle>Friends' Creations</dht:largeTitle>
	
			<div>
				<c:forEach items="${nowplaying.friendsThemes.list}" var="theme" varStatus="status">
					<dht:nowPlayingTheme theme="${theme}" signin="${nowplaying.signin}"/>
				</c:forEach>		
			</div>
		</c:if>
		
		<c:if test="${nowplaying.randomThemes.size > 0}">
			<dht:largeTitle>Recent Themes</dht:largeTitle>
			
			<div>
				<c:forEach items="${nowplaying.randomThemes.list}" var="theme" varStatus="status">
					<dht:nowPlayingTheme theme="${theme}" signin="${nowplaying.signin}"/>
				</c:forEach>		
			</div>
		</c:if>
		
		&nbsp;
		<div>
			Or, <a href="javascript:dh.nowplaying.createNewTheme(null);">create a new theme from scratch</a>
		</div>
		
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>
