<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- this tag is intended to be included only if invites are available --%>
<%@ attribute name="promotion" required="true" type="java.lang.String"%>
<%@ attribute name="invitesAvailable" required="true" type="java.lang.Integer"%>
<%@ attribute name="summitSelfInvite" required="false" type="java.lang.Boolean" %>

<%-- We need to uniquify ids across the generated output --%>
<c:if test="${empty dhSelfInviteCount}">
	<c:set var="dhSelfInviteCount" value="0" scope="request"/>
</c:if>
<c:set var="dhSelfInviteCount" value="${dhSelfInviteCount + 1}" scope="request"/>
<c:set var="N" value="${dhSelfInviteCount}" scope="page"/>

<div>
	<script type="text/javascript">
		dojo.require('dh.util');
		dojo.require('dh.server');
		dojo.require('dh.textinput');
		
		selfInviteAddress${N} = null;
		
		var dhSelfInviteInit${N} = function() {
	        selfInviteAddressNode = document.getElementById('dhSelfInviteAddress${N}');
	        if (selfInviteAddressNode != null)
	            selfInviteAddress${N} = new dh.textinput.Entry(selfInviteAddressNode, "someone@example.com", "");    
		}
			
		var dhSelfInviteComplete${N} = function(message) {
			var messageNode = document.getElementById('dhSelfInviteMessage${N}');
			dh.util.clearNode(messageNode);
			messageNode.appendChild(document.createTextNode(message));
		}
		
		var dhSelfInvite${N} = function() {
			var addressNode = document.getElementById('dhSelfInviteAddress${N}');			
			dh.util.hideId('dhSelfInviteForm${N}');
			dh.util.showId('dhSelfInviteMessage${N}');
			
			// make sure we do not send an e-mail to the e-mail example
			selfInviteAddress${N}.hideDefaultText();
			
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
		
		dojo.event.connect(dojo, "loaded", dj_global, "dhSelfInviteInit${N}");
	</script>
	<div id="dhSelfInviteForm${N}">
		<c:choose>
			<c:when test="${invitesAvailable > 0}">
			    <c:choose>
			        <c:when test="${summitSelfInvite}">
				        <div class="dh-special-subtitle">
		                    You heard, you browsed, you signed up!
		                </div>
				    </c:when>
				    <c:otherwise>
				        <div class="dh-special-subtitle">
				            To sign up for Mugshot, enter your email address.
				            <br/>
					        Then, check your email for a sign-in link.
				        </div>		   
				    </c:otherwise>
				</c:choose>
				    
				<input type="text" class="dh-text-entry" id="dhSelfInviteAddress${N}" size="30"/>
			    <input type="button" value="Send" onclick="dhSelfInvite${N}()"/>
			</c:when>
			<c:otherwise>
		        <div class="dh-special-subtitle">
	        		Temporarily out of invitations
        		</div>
        		<div class="dh-special-subtitle dh-landing-explanatory">
        			Sorry, we've run out of invitations to Mugshot. If you enter your email address below, we'll let you know when we have more available.
        		</div>
        		<dht:wantsIn buttonText="Take a rain check"/>
			</c:otherwise>
		</c:choose>    	
	</div>
	<br/>
	<div id="dhSelfInviteMessage${N}" class="dh-landing-result dhInvisible">
		Thinking...
	</div>
</div>
