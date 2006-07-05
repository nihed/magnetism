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
	<div class="dh-compact-item">
		<table cellpadding="0" cellspacing="0">
			<tbody>
				<tr valign="top">
					<td>
						<div id="dhSidebarBoxProfileGroupImage" class="dh-image">
							<dht:groupshot group="${group.viewedGroup}"/>
						</div>
					</td>
					<td>
						<div class="dh-next-to-image">
							<div id="dhSidebarBoxProfileGroupName" class="dh-name"><c:out value="${group.name}"/></div>
							<c:if test="${group.canModify}">
								<div class="dh-action-link"><a href="/group-account?group=${group.viewedGroupId}" title="Edit the group information">Edit group</a></div>
							</c:if>
							<c:choose>
								<%-- Be careful if changing this not to show both join and leave at the same time --%>
								<c:when test="${!empty group.joinAction}">
									 <dht:actionLink href="javascript:dh.actions.joinGroup('${group.viewedGroupId}')" title="${group.joinTooltip}"><c:out value="${group.joinAction}"/></dht:actionLink>
								</c:when>							
								<c:when test="${!empty group.leaveAction}">
									 <dht:actionLink href="javascript:dh.actions.leaveGroup('${group.viewedGroupId}')" title="${group.leaveTooltip}"><c:out value="${group.leaveAction}"/></dht:actionLink>
								</c:when>
							</c:choose>
							<c:if test="${group.canShare}">
								 <dht:actionLink href="/group-invitation?group=${group.viewedGroupId}" title="Invite other people to this group">Invite People</dht:actionLink>
							</c:if>
							<c:if test="${group.member}">
								<dht:actionLinkChat chatId="${group.viewedGroupId}" kind="group"/>
							</c:if>								
						</div>
					</td>
				</tr>
			</tbody>
		</table>
		<div id="dhSidebarBoxProfileGroupDescription" class="dh-bio">
			<c:choose> 
				<c:when test="${!empty group.viewedGroup.group.description}">
					<c:out value="${group.viewedGroup.group.description}"/>
				</c:when>
				<c:when test="${group.member}">
					<div class="dh-action-link"><a href="/group-account?group=${group.viewedGroupId}" title="Edit the group information">Enter a description</a></div>
				</c:when>
			</c:choose>
		</div>
		<c:if test="${!empty group.feeds.list}">
			<dht:sidebarBoxSeparator/>
			<div id="dhSidebarBoxProfileGroupFeeds" class="di-bio">
			<strong>Group Feeds</strong>
			<ul class="dh-group-feed-list">
				<c:forEach items="${group.feeds.list}" var="feed">
					<li class="dh-group-feed-item"><a href="${feed.feed.link.url}" target="_blank"><c:out value="${feed.feed.title}"/></a></li>
				</c:forEach>				  
			</ul>
			</div>
		</c:if>
	</div>
</dht:sidebarBox>
