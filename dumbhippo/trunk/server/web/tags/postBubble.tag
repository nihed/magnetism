<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="info" required="true" type="com.dumbhippo.server.PostInfo"%>

<div class="cool-bubble-shadow">		
	<table class="cool-bubble">
	<tr>
	<td class="cool-person" rowSpan="3">
		<a class="cool-person" href="">
			<img class="cool-person" src="/files/headshots/${info.posterInfo.person.id}" />
			<br/>
			<dh:entity value="${info.posterInfo}"/>
		</a>
	</td>
	<td class="cool-link">
		<div class="cool-link">
			<c:choose>
				<c:when test="${info.viewerHasViewed}">
					<c:set var="linkcss" value="cool-link-viewed" scope="page"/>
				</c:when>
				<c:otherwise>
					<c:set var="linkcss" value="cool-link" scope="page"/>
				</c:otherwise>
			</c:choose>
			<a class="${linkcss}" title="${info.url}" href="/jsp/frameset.jsp?postId=${info.post.id}"><c:out value="${info.title}"/></a>
		</div>
	</td>
	</tr>
	<tr>
	<td class="cool-link-desc">
		<c:out value="${info.post.textAsHtml}" escapeXml="false"/>
	</td>
	</tr>
	<tr>
	<td class="cool-link-meta">
		<div class="cool-link-date">
		(<fmt:formatDate value="${info.post.postDate}" type="both"/>)
		</div>
		<div class="cool-link-to">
			<dh:entityList value="${info.recipients}"/>
		</div>
	</td>
	</tr>
	</table>
</div>
