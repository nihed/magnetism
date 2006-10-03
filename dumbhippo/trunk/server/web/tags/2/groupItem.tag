<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="group" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="controls" required="false" type="java.lang.Boolean"%>

<div class="dh-compact-item">
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
					<div class="dh-image">
						<dht:groupshot group="${group}"/>
					</div>
				</td>
				<td>
					<div class="dh-next-to-image">
						<div class="dh-name"><a href="/group?who=${group.group.id}"><c:out value="${group.group.name}"/></a></div>
						<c:choose>
							<c:when test="${controls && group.invited || controls && group.invitedToFollow}">
								<div id="dhGroupInvitationControls-${group.group.id}">
									<div class="dh-group-invitation-control" id="dhGroupInvitationAccept-${group.group.id}">
											<img class="dh-group-add" src="/images2/${buildStamp}/add.png"
											  onclick="javascript:dh.groups.joinGroup('${group.group.id}')">
											  <a id="dhGroupInvitationAcceptLink-${group.group.id}" 
											  	href="javascript:dh.groups.joinGroup('${group.group.id}')">Accept</a>
									</div>
									<div class="dh-group-invitation-action-complete" style="display: none;" id="dhGroupInvitationAccepted-${group.group.id}">
									Accepted
									</div>
									<div class="dh-group-invitation-control" id="dhGroupInvitationDecline-${group.group.id}">
											<img class="dh-group-leave" src="/images2/${buildStamp}/block.png"
											  onclick="javascript:dh.groups.leaveGroup('${group.group.id}')">
											  <a id="dhGroupInvitationDeclineLink-${group.group.id}"
												  href="javascript:dh.groups.leaveGroup('${group.group.id}')">Decline</a>
									</div>
									<div class="dh-group-invitation-action-complete" style="display: none;" id="dhGroupInvitationDeclined-${group.group.id}">
									Declined
									</div>									
								</div>
								<div style="display: none;" id="dhGroupInvitationWorking-${group.group.id}">Working...</div>
							</c:when>
							<c:otherwise>
								<div class="dh-info"><c:out value="${group.liveGroup.memberCount}"/> members</div>
								<div class="dh-info"><c:out value="${group.liveGroup.totalReceivedPosts}"/> posts</div>							
							</c:otherwise>
						</c:choose>
					</div>
				</td>
			</tr>
		</tbody>
	</table>
</div>
