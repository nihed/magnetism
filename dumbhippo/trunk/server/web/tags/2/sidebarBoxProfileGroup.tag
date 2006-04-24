<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:choose>
	<c:when test="${group.public}">
		<c:set var="title" value="PUBLIC GROUP" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="title" value="PRIVATE GROUP" scope="page"/>
	</c:otherwise>
</c:choose>

<dht:sidebarBox boxClass="dh-profile-box dh-profile-group-box" title="${title}" lockIcon="${!group.public}">
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
							<div class="dh-name"><c:out value="${group.name}"/></div>
							<div class="dh-action-link"><a href="FIXME">Edit profile</a></div>
							<div class="dh-action-link"><a href="FIXME">Leave group</a></div>
							<div class="dh-action-link"><a href="FIXME">Log out</a></div>
						</div>
					</td>
				</tr>
			</tbody>
		</table>
		<div class="dh-bio">
			Damn, this group is cool. Raise the goblet of rock.
		</div>
	</div>
</dht:sidebarBox>
