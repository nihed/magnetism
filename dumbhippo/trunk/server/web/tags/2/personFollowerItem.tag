<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.PersonView" %>

<dht:personItem who="${who}" invited="true" suppressDefaultBody="true">
	<div class="dh-follower-actions">
		<dht:asyncActionLink 
			exec="dh.actions.addMember('${group.viewedGroupId}', '${who.user.id}', function () { dh.asyncActionLink.complete('addMember${group.viewedGroupId}${who.user.id}') })"
			ctrlId="addMember${group.viewedGroupId}${who.user.id}"
			text="Invite to group"
			completedText="Invited to group"/>							
		</div>
</dht:personItem>
