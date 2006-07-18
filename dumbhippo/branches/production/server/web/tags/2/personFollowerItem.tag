<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="group" required="false" type="com.dumbhippo.web.pages.GroupPage" %>
<%@ attribute name="who" required="true" type="com.dumbhippo.server.PersonView" %>

<c:choose>
	<c:when test="${(empty group) || group.canModify}">
		<dht:personItem who="${who}" invited="true" suppressDefaultBody="true">
			<div class="dh-follower-actions">
		        <c:choose>
		            <c:when test="${empty group}">
				        <dht:asyncActionLink 
					        exec="dh.actions.addContact('${who.user.id}', function () { dh.asyncActionLink.complete('addContact${who.user.id}') })"
					        ctrlId="addContact${who.user.id}"
					        text="Add to your friends"
					        completedText="Added"/>			            
					</c:when>
					<c:otherwise>
				        <dht:asyncActionLink 
					        exec="dh.actions.addMember('${group.viewedGroupId}', '${who.user.id}', function () { dh.asyncActionLink.complete('addMember${group.viewedGroupId}${who.user.id}') })"
					        ctrlId="addMember${group.viewedGroupId}${who.user.id}"
					        text="Invite to group"
					        completedText="Invited to group"/>
					</c:otherwise>
				</c:choose>	        							
			</div>
		</dht:personItem>
	</c:when>
	<c:otherwise>
		<dht:personItem who="${who}"/>
	</c:otherwise>
</c:choose>
