<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.PostView"%>

<div class="cool-bubble-shadow">		
	<table class="cool-bubble">
	<tr>
	<td class="cool-person" rowSpan="3">
		<a class="cool-person" href="">
			<img class="cool-person" src="/files/headshots/${post.poster.person.id}" />
			<br/>
			<dh:entity value="${post.poster}"/>
		</a>
	</td>
	<td class="cool-link">
		<div class="cool-link">
			<c:choose>
				<c:when test="${post.viewerHasViewed}">
					<c:set var="linkcss" value="cool-link-viewed" scope="page"/>
				</c:when>
				<c:otherwise>
					<c:set var="linkcss" value="cool-link" scope="page"/>
				</c:otherwise>
			</c:choose>
			<a class="${linkcss}" title="${post.url}" href="frameset?postId=${post.post.id}"><c:out value="${post.title}"/></a>
		</div>
	</td>
	</tr>
	<tr>
	<td class="cool-link-desc">
		<c:out value="${post.post.textAsHtml}" escapeXml="false"/>
	</td>
	</tr>
	<tr>
	<td class="cool-link-meta">
		<div class="cool-link-date">
		(<fmt:formatDate value="${post.post.postDate}" type="both"/>)
		</div>
		<div class="cool-link-to">
			<dh:entityList value="${post.recipients}"/>
		</div>
	</td>
	</tr>
	</table>
</div>
