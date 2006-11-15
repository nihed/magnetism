<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%-- this tag is intended to be included only if invites are available --%>
<%@ attribute name="promotion" required="true" type="java.lang.String"%>
<%@ attribute name="invitesAvailable" required="true" type="java.lang.Integer"%>

<%-- We need to uniquify ids across the generated output --%>
<c:if test="${empty dhSelfInviteCount}">
	<c:set var="dhSelfInviteCount" value="0" scope="request"/>
</c:if>
<c:set var="dhSelfInviteCount" value="${dhSelfInviteCount + 1}" scope="request"/>
<c:set var="N" value="${dhSelfInviteCount}" scope="page"/>

<div>
	<script type="text/javascript">
		var dhSelfInviteComplete${N} = function(message) {
			var messageNode = document.getElementById('dhSelfInviteMessage${N}');
			dh.util.clearNode(messageNode);
			messageNode.appendChild(document.createTextNode(message));
		}
		var dhSelfInvite${N} = function() {
			var addressNode = document.getElementById('dhSelfInviteAddress${N}');
			dh.util.hideId('dhSelfInviteForm${N}');
			dh.util.showId('dhSelfInviteMessage${N}');
		   	dh.server.getXmlPOST("inviteself",
			     {
			     	"address" : addressNode.value,
			     	"promotion" : "${promotion}"
			     },
	  	    	 function(type, data, http) {
	  	    	 	var messageElements = data.getElementsByTagName("message");
	  	    	 	if (!messageElements)
	  	    	 		text = "Something went wrong... (1)";
	  	    	 	else {
						var messageElement = messageElements.item(0);
						if (!messageElement)
							text = "Something went wrong... (2)";
						else
							text = messageElement ? dojo.dom.textContent(messageElement) : "Something went wrong... (3)";
					}
					dhSelfInviteComplete${N}(text);
	  	    	 },
	  	    	 function(type, error, http) {
	  	    	    dhSelfInviteComplete${N}("Something went wrong! Reload the page and try again.");
	  	    	 });
		}
	</script>
	<div id="dhSelfInviteForm${N}">
		<b>Your email address:</b>
		<input type="text" id="dhSelfInviteAddress${N}"/>
		<input type="button" value="Invite Yourself" onclick="dhSelfInvite${N}()"/>
		<div>
			<br/>
			There's room for <c:out value="${invitesAvailable}"/> more people.
			Tell your friends!
		</div>
	</div>
	<br/>
	<div id="dhSelfInviteMessage${N}" class="dhInvisible" style="font-weight: bold;">
		Thinking...
	</div>
</div>
