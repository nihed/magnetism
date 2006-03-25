<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="theme" required="true" type="com.dumbhippo.persistence.NowPlayingTheme"%>
<%@ attribute name="signin" required="true" type="com.dumbhippo.web.SigninBean"%>

<div>
	<c:if test="${theme.draft}">
		<b>DRAFT</b>
	</c:if>
	'<c:out value="${theme.name}"/>' by <c:out value="${theme.creator.nickname}"/>
	<c:if test="${!empty theme.basedOn}">
		based on '<c:out value="${theme.basedOn.name}"/>' by <c:out value="${theme.basedOn.creator.nickname}"/>
	</c:if>
	<br/>
	<dh:nowPlaying userId="${signin.userId}" themeId="${theme.id}"/>
	<br/>
	<c:if test="${theme.draft && (theme.creator eq signin.user)}">
		<a href="/nowplaying-theme-creator?theme=${theme.id}">Edit</a>
	</c:if>
	<a href="javascript:dh.nowplaying.createNewTheme('${theme.id}');">Build On It</a>
	<c:if test="${!theme.draft}">
		<a href="javascript:dh.nowplaying.setTheme('${theme.id}');">Use As Current Theme</a>
	</c:if>
</div>
