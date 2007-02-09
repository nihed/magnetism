<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@ attribute name="id" required="true" type="java.lang.String" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="icon" required="true" type="java.lang.String" %>
<%@ attribute name="bodyHtml" required="false" type="java.lang.String" %>

<dht:popup id="${id}" extraClass="dh-feed-popup">
	<div>
		<table cellpadding="0" cellspacing="0">
			<tbody>
				<tr>
					<td valign="top">
						<div class="dh-left-side">
							<c:choose>
								<c:when test="${fn:endsWith(icon, '.gif')}">
									<img src="${icon}" style="width: 21; height: 20;"/>
								</c:when>
								<c:otherwise>
									<dh:png src="${icon}" style="width: 21; height: 20;"/>
								</c:otherwise>
							</c:choose>
						</div>
					</td>
					<td>
						<div class="dh-title"><c:out value="${title}"/></div>
						<div class="dh-subtitle"><%-- user-provided url gets inserted here from Javascript --%></div>
						<c:out value="${bodyHtml}" escapeXml="false"/>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
	<div class="dh-controls">
		<jsp:doBody/>
	</div>
</dht:popup>
