<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="shortVersion" required="false" type="java.lang.Boolean" %>

<c:if test="${empty shortVersion}">
	<c:set var="shortVersion" value="false"/>
</c:if>

<div class="dh-person-header">
    <table class="dh-person-info">
    <tbody>
    <tr valign="top">
    <td>
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
					<div class="dh-image">
						<dht:groupshot group="${who}" size="60"/>
					</div>
				</td>
				<td>
					<div class="dh-person-header-next-to-image">
						<span class="dh-person-header-name"><a href="${who.homeUrl}"><c:out value="${who.name}"/></a></span>
							
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
			        <c:if test="${!shortVersion}">
			            <div class="dh-person-header-bio">
				            <c:out value="${who.group.description}"/>
			            </div>
			        </c:if>
                </td>
            </tr>             
		</tbody>
	</table>
	</td>
	<td align="right">
	</td>
	</tr>
	</tbody>
	</table>
</div>
