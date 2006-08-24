<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="theme" required="true" type="com.dumbhippo.persistence.NowPlayingTheme"%>
<%@ attribute name="signin" required="true" type="com.dumbhippo.web.SigninBean"%>
<%@ attribute name="alreadyCurrent" required="false" type="java.lang.Boolean"%>
<%@ attribute name="userId" required="false" type="java.lang.String" %>

<%-- default to our own userId if none given --%>
<c:if test="${empty userId}">
	<c:set var="userId" value="${signin.userId}" scope="page"/>
</c:if>

<div>
	<c:if test="${theme.draft}">
		<b>DRAFT</b>
	</c:if>
	'<c:out value="${theme.name}"/>' by <c:out value="${theme.creator.nickname}"/>
	<c:if test="${!empty theme.basedOn}">
		based on
		<%-- <a href="/radar-theme?theme=${theme.basedOn.id}">--%>'<c:out value="${theme.basedOn.name}"/>'
		by <c:out value="${theme.basedOn.creator.nickname}"/><%--</a>--%>
	</c:if>
	<br/>
	<dh:nowPlaying userId="${userId}" themeId="${theme.id}" hasLabel="false"/>
	<br/>
	<c:if test="${signin.valid}">
		<span class="dh-option-list">	
		<c:if test="${theme.creator eq signin.user}">
			<a class="dh-option-list-option" href="/radar-theme-creator?theme=${theme.id}">Edit</a>
			|
		</c:if>
		<a class="dh-option-list-option" href="javascript:dh.nowplaying.createNewTheme('${theme.id}');">Build On It</a>
		<c:if test="${!theme.draft && !alreadyCurrent}">
			|
			<a class="dh-option-list-option" href="javascript:dh.nowplaying.setTheme('${theme.id}');">Set As Current Theme</a>
		</c:if>
		</span>
	</c:if>
</div>
