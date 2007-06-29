<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.GroupMemberBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-orange2' : 'dh-box-orange1'}" blockId="${blockId}">
	<dht3:blockLeft block="${block}" oneLine="${oneLine}">
		<dht3:blockTitle>
			<span class="dh-stacker-block-title-group-member-name"><dht3:entityLink who="${block.memberView}"/></span>
			<c:choose>
				<c:when test="${dh:enumIs(block.status, 'FOLLOWER')}"> is a new follower of the group </c:when>
				<c:when test="${dh:enumIs(block.status, 'ACTIVE')}"> is a new member of the group </c:when>
				<c:when test="${dh:enumIs(block.status, 'INVITED')}"> is invited to the group </c:when>
				<c:when test="${dh:enumIs(block.status, 'INVITED_TO_FOLLOW')}"> is invited to follow the group </c:when>
				<c:when test="${block.stoppedFollowing}"> stopped following the group </c:when>
				<c:otherwise> left the group </c:otherwise>
			</c:choose>
			<dht3:entityLink who="${block.groupView}"/>
		</dht3:blockTitle>
		<dht3:blockDescription blockId="${blockId}" literalBody="true">
			<c:choose>
				<c:when test="${block.viewerCanInvite}">
					<img src="/images2/${buildStamp}/add.png"/>	
					<dh:script module="dh.actions"/>
					<dht:asyncActionLink 
						tagName="span"
						exec="dh.actions.addMember('${block.groupView.group.id}', '${block.memberView.user.id}', function () { dh.asyncActionLink.complete('addMember${block.groupView.group.id}${block.memberView.user.id}') })"
						ctrlId="addMember${block.groupView.group.id}${block.memberView.user.id}"
						text="Invite to group"
						completedText="Invited to group"/>	
				</c:when>
				<c:otherwise>
					<c:if test="${fn:length(block.adders) > 0}">
						Invited by 
						<c:forEach items="${block.adders}" var="adder" varStatus="adderIdx">
							 <dht3:entityLink who="${adder}"/><c:if test="${!adderIdx.last}">,</c:if>
						</c:forEach>
					</c:if>
				</c:otherwise>
			</c:choose>	
		</dht3:blockDescription>				
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.groupView}" showFrom="${showFrom}" oneLine="${oneLine}">
		<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>		
	</dht3:blockRight>
</dht3:blockContainer>

