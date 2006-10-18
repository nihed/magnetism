<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.GroupMemberBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-orange2' : 'dh-box-orange1'}" blockId="${blockId}">
	<dht3:blockHeader icon="/images3/${buildStamp}/mugshot_icon.png" blockId="${blockId}">
		<dht3:blockHeaderLeft>
			<span class="dh-stacker-block-title-group-member-name"><dht3:entityLink who="${block.memberView}"/></span> is a new 
			<c:choose>
				<c:when test="${dh:enumIs(block.status, 'FOLLOWER')}">follower.</c:when>
				<c:otherwise>member.</c:otherwise>
			</c:choose>		
			<dht3:blockHeaderDescription blockId="${blockId}">
				<c:choose>
					<c:when test="${dh:enumIs(block.status, 'FOLLOWER')}">
						<img src="/images2/${buildStamp}/add.png"/>	
						<dht:asyncActionLink 
							tagName="span"
							exec="dh.actions.addMember('${block.groupView.group.id}', '${block.memberView.user.id}', function () { dh.asyncActionLink.complete('addMember${block.groupView.group.id}${block.memberView.user.id}') })"
							ctrlId="addMember${group.groupView.group.id}${block.memberView.user.id}"
							text="Invite to group"
							completedText="Invited to group"/>	
					</c:when>
					<c:otherwise>
						Invited by <dht3:entityLink who="${block.memberView}"/>.
					</c:otherwise>
				</c:choose>	
			</dht3:blockHeaderDescription>				
		</dht3:blockHeaderLeft>
		<dht3:blockHeaderRight blockId="${blockId}" from="${block.groupView}" showFrom="${showFrom}">
			<dht3:blockTimeAgo block="${block}"/>		
		</dht3:blockHeaderRight>
	</dht3:blockHeader>	
	<dht3:blockContent blockId="${blockId}">
	</dht3:blockContent>
</dht3:blockContainer>

