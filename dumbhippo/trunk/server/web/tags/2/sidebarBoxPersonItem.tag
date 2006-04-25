<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.PersonView" %>

<c:if test="${!empty who.viewPersonPageId}">
	<c:set var="personLink" value="/person?who=${who.viewPersonPageId}" scope="page"/>
</c:if>	

<div class="dh-item">
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
					<div class="dh-image">
						<dht:headshot person="${who}"/>
					</div>
				</td>
				<td>
					<div class="dh-next-to-image">
						<div class="dh-name">
							<c:choose>
								<c:when test="${!empty personLink}">
									<a href="${personLink}"><c:out value="${who.name}"/></a>
								</c:when>
								<c:otherwise>
									<c:out value="${who.name}"/>
								</c:otherwise>
							</c:choose>
						</div>
						<div class="dh-info">FIXME groups</div>
						<div class="dh-info">FIXME posts</div>
					</div>
				</td>
			</tr>
		</tbody>
	</table>
</div>
