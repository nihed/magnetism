<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.pages.NowPlayingThemeCreatorPage" scope="request"/>
<jsp:setProperty name="nowplaying" property="themeId" param="theme"/>

<c:if test="${empty nowplaying.theme}">
	<dht:errorPage>No theme!</dht:errorPage>
</c:if>

<head>
        <title>Mugshot - Radar Theme '<c:out value="${nowplaying.theme.name}"/>'</title>
		<dht3:stylesheet name="site" iefixes="true"/>        
		<dht:faviconIncludes/>
		<dh:script module="dh.nowplaying"/>
</head>

<dht3:page currentPageLink="radar-theme">
	<dht3:pageSubHeader title="Radar Theme '${dh:xmlEscape(nowplaying.theme.name)}'"/>
	<dht3:shinyBox color="grey">
		<div>While Music Is Playing</div>
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="active" hasLabel="false"/>
		</div>
		
		<div>When Not Listening</div>		
		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="inactive" hasLabel="false"/>
		</div>
		
		<div>
			<c:if test="${nowplaying.theme.creator eq signin.user}">
				<a href="/radar-theme-creator?theme=${nowplaying.theme.id}">Edit</a><br/>
			</c:if>
			<c:if test="${!nowplaying.theme.draft}">
				<a href="javascript:dh.nowplaying.setTheme('${nowplaying.theme.id}');">Set As Current Theme</a>
			</c:if>
		</div>
		
		<c:if test="${!empty nowplaying.theme.basedOn}">
			<div>
				'<c:out value="${nowplaying.theme.name}"/>' by <c:out value="${nowplaying.theme.creator.nickname}"/> based on
				<a href="/radar-theme?theme=${nowplaying.theme.basedOn.id}">'<c:out value="${nowplaying.theme.basedOn.name}"/>' by
				<c:out value="${nowplaying.theme.basedOn.creator.nickname}"/></a>
			</div>
		</c:if>

		<div>
			<a href="/radar-themes">More Themes</a>
		</div>
			
	</dht3:shinyBox>
	
</dht3:page>
</html>
