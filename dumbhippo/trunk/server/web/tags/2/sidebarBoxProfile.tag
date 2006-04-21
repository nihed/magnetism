<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:choose>
	<c:when test="${sidebar.self}">
		<c:set var="title" value="MY PROFILE" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="title" value="PROFILE" scope="page"/>
	</c:otherwise>
</c:choose>

<dht:sidebarBox boxClass="dh-profile-box" title="${title}">
	<div class="dh-item">
		<table cellpadding="0" cellspacing="0">
			<tbody>
				<tr valign="top">
					<td>
						<div class="dh-image">
						</div>
					</td>
					<td>
						<div class="dh-next-to-image">
							<div class="dh-name"><c:out value="${sidebar.viewedPerson.name}"/></div>
							<div class="dh-action-link"><a href="/account">Edit account</a></div>
							<div class="dh-action-link"><a href="FIXME">Sign out</a></div>
						</div>
					</td>
				</tr>
			</tbody>
		</table>
		<div class="dh-bio">
			I am so special. This is my biography. Please read more about me!
		</div>
	</div>
</dht:sidebarBox>
