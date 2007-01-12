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
	<c:if test="${alreadyCurrent}"><em>My Current Theme</em>:</c:if>
	<strong><c:out value="${theme.name}"/></strong> by <a href="/person?who=${theme.creator.id}"><c:out value="${theme.creator.nickname}"/></a>
	<c:if test="${!empty theme.basedOn}">
		based on
		<%-- <a href="/radar-theme?theme=${theme.basedOn.id}">--%><strong><c:out value="${theme.basedOn.name}"/></strong><%--</a>--%>
		by <a href="/person?who=${theme.basedOn.creator.id}"><c:out value="${theme.basedOn.creator.nickname}"/></a>
	</c:if>
	<br/>
	<table><tr><td rowSpan="5"><dh:nowPlaying userId="${userId}" themeId="${theme.id}" hasLabel="false"/></td></tr>
	<c:if test="${theme.draft}">
		<tr><td><b style="color:red">draft</b> <span style="font-size:10px">(you could <a href="javascript:dh.nowplaying.modify('${theme.id}', 'draft', 'false', '/radar-themes');">publish</a> it)</span></td></tr>
	</c:if>
	<c:if test="${signin.valid}">
		<c:if test="${theme.creator eq signin.user}">
			<tr><td><a href="/radar-theme-creator?theme=${theme.id}">Edit</a></td></tr>
		</c:if>
		<tr><td><a href="javascript:dh.nowplaying.createNewTheme('${theme.id}');">Build On It</a></td></tr>
		<c:if test="${!theme.draft && !alreadyCurrent}">
			<tr><td><a href="javascript:dh.nowplaying.setTheme('${theme.id}');">Set As Current Theme</a></td></tr>
		</c:if>
	</c:if>
	</table>
</div>
