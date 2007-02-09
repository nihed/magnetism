<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="anchor" required="false" type="java.lang.String" %>
<%@ attribute name="disableLink" required="false" type="java.lang.Boolean" %>

<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if>

<td>
<table cellpadding="0" cellspacing="0">
	<tbody>
		<tr valign="top">
			<td>
				<div class="dh-image">
					<dht:groupshot group="${who}" size="60" disableLink="${disableLink}"/>
				</div>
			</td>
			<td>
				<div class="dh-person-header-next-to-image">
					<span class="dh-person-header-name">
						<c:choose>
							<c:when test="${!empty anchor}">
								<a href="${who.homeUrl}" name="${anchor}"><c:out value="${who.name}"/></a>
							</c:when>
							<c:otherwise>
								<a href="${who.homeUrl}"><c:out value="${who.name}"/></a>
							</c:otherwise>
						</c:choose>
					</span>
					<c:if test="${!embedVersion}">
						<span class="dh-person-header-description">Group activity</span>
					</c:if>								
					<div class="dh-person-header-controls"><jsp:doBody/></div>
					<div class="dh-person-header-stats">
						<c:if test="${who.liveGroup != null}">
							<span class="dh-info">${who.liveGroup.memberCount} members</span> |
							<span class="dh-info">${who.liveGroup.followerCount} followers</span> | 
							<span class="dh-info">${who.liveGroup.totalReceivedPosts} posts</span> 
						</c:if>
					</div>
				</div>
			</td>
		</tr>
		<tr>
		    <td colspan="2">
		        <c:if test="${!embedVersion}">
		            <div class="dh-person-header-bio">
			            <c:out value="${who.group.description}"/>
		            </div>
		        </c:if>
               </td>
           </tr>             
	</tbody>
</table>
</td>
