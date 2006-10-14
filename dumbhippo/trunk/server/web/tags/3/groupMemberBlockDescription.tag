<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.GroupMemberBlockView" %>

<c:choose>
	<c:when test="${dh:enumIs(block.status, 'FOLLOWER')}">
		<img src="/images2/${buildStamp}/add.png"/>	
		<dht:asyncActionLink 
			tagName="span"
			exec="dh.actions.addMember('${group.viewedGroupId}', '${who.user.id}', function () { dh.asyncActionLink.complete('addMember${group.viewedGroupId}${who.user.id}') })"
			ctrlId="addMember${group.viewedGroupId}${who.user.id}"
			text="Invite to group"
			completedText="Invited to group"/>	
	</c:when>
	<c:otherwise>
		Invited by <dht3:personLink who="${block.memberView}"/>.
	</c:otherwise>
</c:choose>
