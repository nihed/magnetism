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
							<dht:headshot person="${sidebar.viewedPerson}"/>
						</div>
					</td>
					<td>
						<div class="dh-next-to-image">
							<div class="dh-name"><c:out value="${sidebar.viewedPerson.name}"/></div>
							<dht:actionLink href="/account" title="Set preferences, add addresses, and update your profile">Edit account</dht:actionLink>
							<dht:actionLinkLogout/>
						</div>
					</td>
				</tr>
			</tbody>
		</table>
		<c:if test="${!empty sidebar.viewedPerson.bioAsHtml}">
			<div class="dh-bio">
				<c:out value="${sidebar.viewedPerson.bioAsHtml}" escapeXml="false"/>
			</div>
		</c:if>
	</div>
</dht:sidebarBox>
