<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="invited" required="false" type="java.lang.Boolean" %>
<%@ attribute name="suppressDefaultBody" required="false" type="java.lang.Boolean" %>

<div class="dh-compact-item">
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
					<div class="dh-image">
						<dht:headshot person="${who}" invited="${invited}"/>
					</div>
				</td>
				<td>
					<div class="dh-next-to-image">
						<dht:personName who="${who}"/>
						<c:choose>
							<c:when test="${!suppressDefaultBody}">
								<c:if test="${who.liveUser != null}">
									<div class="dh-info">${who.liveUser.groupCount} groups</div>
									<div class="dh-info">${who.liveUser.sentPostsCount} posts</div> 
								</c:if>
							</c:when>
							<c:otherwise>
								<jsp:doBody/>						
							</c:otherwise>
						</c:choose>
					</div>
				</td>
			</tr>
		</tbody>
	</table>
</div>
