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
							<dht:groupshot group="${group.viewedGroup}"/>
						</div>
					</td>
					<td>
						<div class="dh-next-to-image">
							<div class="dh-name"><c:out value="${group.name}"/></div>
							<div class="dh-action-link"><a href="FIXME" title="Edit the group information">Edit profile</a></div>
							<c:choose>
								<c:when test="${group.canLeave}">
									 <div class="dh-action-link"><a href='javascript:dh.actions.leaveGroup("${group.viewedGroupId}")' title="I can't take it anymore! Let yourself out of this group.">Leave Group</a></div>
								</c:when>
								<c:when test="${group.canJoin}">
									 <div class="dh-action-link"><a href='javascript:dh.actions.joinGroup("${group.viewedGroupId}")' title="Become a group member">Join Group</a></div>
								</c:when>
							</c:choose>
						   <%-- The browser.gecko check is here because the dynamic hiding of
						        the control when the chat object fails to load doesn't work
						        correctly in firefox 1.0 --%>
						   	<c:if test="${signin.valid && !browser.gecko}">
								<div class="dh-action-link"><a
								href="javascript:dh.actions.requestJoinRoom('${signin.userId}','${group.viewedGroupId}')" title="Chat with other group members">Join Chat</a></div>
							</c:if>
							<div class="dh-action-link"><a href="javascript:dh.actions.signOut();" title="Keep others from using your account on this computer">Log out</a></div>
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
