<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.String" %>
<%@ attribute name="blocks" required="true" type="java.util.List" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>

<dht3:shinyBox color="orange">
    <dht3:groupHeaderContainer>
    	<dht3:groupHeaderLeft who="${who}" embedVersion="true"></dht3:groupHeaderLeft>
    	<dht3:groupHeaderRight>
			<div id="dhGroupInvitationControls-${who.group.id}">
				<div class="dh-group-invitation-control" id="dhGroupInvitationAccept-${who.group.id}">
						<img class="dh-group-add" src="/images2/${buildStamp}/add.png"
						  onclick="javascript:dh.groups.joinGroup('${who.group.id}')">
						  <a id="dhGroupInvitationAcceptLink-${who.group.id}" 
						  	href="javascript:dh.groups.joinGroup('${who.group.id}')">Accept</a>
				</div>
				<div class="dh-group-invitation-action-complete" style="display: none;" id="dhGroupInvitationAccepted-${who.group.id}">
				Accepted
				</div>
				<div class="dh-group-invitation-control" id="dhGroupInvitationDecline-${who.group.id}">
						<img class="dh-group-leave" src="/images2/${buildStamp}/block.png"
						  onclick="javascript:dh.groups.leaveGroup('${who.group.id}')">
						  <a id="dhGroupInvitationDeclineLink-${who.group.id}"
							  href="javascript:dh.groups.leaveGroup('${who.group.id}')">Decline</a>
				</div>
				<div class="dh-group-invitation-action-complete" style="display: none;" id="dhGroupInvitationDeclined-${who.group.id}">
				Declined
				</div>									
			</div>
			<div style="display: none;" id="dhGroupInvitationWorking-${who.group.id}">Working...</div>			    	
		</dht3:groupHeaderRight>
    </dht3:groupHeaderContainer>
	<dht3:stacker blocks="${blocks}" stackOrder="${stackOrder}" showFrom="${showFrom}"/>
</dht3:shinyBox>