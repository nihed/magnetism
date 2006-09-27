<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.PersonView" %>
<%@ attribute name="isSelf" required="true" type="java.lang.Boolean" %>

<div class="dh-person-header">
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
					<div class="dh-image">
						<dht:headshot person="${who}" invited="false"/>
					</div>
				</td>
				<td>
					<div class="dh-person-header-next-to-image">
						<span class="dh-presence">
							<c:choose>
								<c:when test="${who.online}">
									<dh:png src="/images3/${buildStamp}/online_icon.png" style="width: 12px; height: 12px;"/>
								</c:when>
								<c:otherwise>
									<dh:png src="/images3/${buildStamp}/offline_icon.png" style="width: 12px; height: 12px;"/>
								</c:otherwise>
							</c:choose>
						</span>			
						<c:choose>
							<c:when test="${isSelf}">
							<span class="dh-person-header-name"><c:out value="${who.name}"/>'s Mugshot</span>
							</c:when>
							<c:otherwise>
							<span class="dh-person-header-name"><a href="/person?who=${who.viewPersonPageId}"><c:out value="${who.name}"/></a>'s Mugshot</span>							
							</c:otherwise>
						</c:choose>
							
						<div class="dh-person-header-controls"><jsp:doBody/></div>
						<div class="dh-person-header-stats">
							<c:if test="${who.liveUser != null}">
								<span class="dh-info">${who.liveUser.contactResourcesSize} friends</span> | 							
								<span class="dh-info">${who.liveUser.groupCount} groups</span> | 
								<span class="dh-info">${who.liveUser.sentPostsCount} posts</span> 
							</c:if>
						</div>
					</div>
				</td>
			</tr>
			<tr>
			<td colspan="2">
			<div class="dh-person-header-bio">
				${who.bioAsHtml}
			</div>
			</td>
			</tr>
		</tbody>
	</table>
</div>
