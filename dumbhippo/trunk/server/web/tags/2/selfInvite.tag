<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- this tag is intended to be included only if invites are available --%>
<%@ attribute name="promotion" required="true" type="java.lang.String"%>
<%@ attribute name="invitesAvailable" required="true" type="java.lang.Integer"%>

<%-- We need to uniquify ids across the generated output --%>
<c:if test="${empty dhSelfInviteCount}">
	<c:set var="dhSelfInviteCount" value="0" scope="request"/>
</c:if>
<c:set var="dhSelfInviteCount" value="${dhSelfInviteCount + 1}" scope="request"/>
<c:set var="N" value="${dhSelfInviteCount}" scope="page"/>

<dh:script modules="dh.actions,dh.util,dh.event"/>
<div>
	<script type="text/javascript">		
		selfInviteAddress${N} = null;
		
		var dhSelfInviteInit${N} = function() {
	        selfInviteAddressNode = document.getElementById('dhSelfInviteAddress${N}');
	        if (selfInviteAddressNode != null) {
	            selfInviteAddress${N} = new dh.textinput.Entry(selfInviteAddressNode, "someone@example.com", "");    
	            selfInviteAddress${N}.onkeyup = function(event) {
	            	if (dh.event.getKeyCode(event) == ENTER)
	            		dhSelfInvite${N}();
	            }
	        }
		}
			
		var dhSelfInviteComplete${N} = function(message) {
			var messageNode = document.getElementById('dhSelfInviteMessage${N}');
			dh.util.clearNode(messageNode);
			messageNode.appendChild(document.createTextNode(message));
		}
		
		var dhSelfInvite${N} = function() {
			var address = selfInviteAddress${N}.getValue();
			if (!dh.util.validateEmail(address))
				return;
			
			dh.util.hideId('dhSelfInviteForm${N}');
			dh.util.showId('dhSelfInviteMessage${N}');

		   	dh.server.getXmlPOST("inviteself",
			     {
			     	"address" : address,
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
							if (text.indexOf(" already has an account") > 0 && dh.util.exists('dhLoginAddressEntry')) {
							    email = text.substring(0, text.indexOf(" already has an account"));
							    document.getElementById('dhLoginAddressEntry').value = email;    
							}
					}
					dhSelfInviteComplete${N}(text);
	  	    	 },
	  	    	 function(type, error, http) {
	  	    	    dhSelfInviteComplete${N}("Something went wrong! Reload the page and try again.");
	  	    	 });
		}
		
		dh.event.addPageLoadListener(dhSelfInviteInit${N});
	</script>
	<div id="dhSelfInviteForm${N}">
		<c:choose>
			<c:when test="${invitesAvailable > 0}">
				<div class="dh-special-subtitle">
				    To sign up for <c:out value="${site.siteName}"/>, enter your email address.
					Then, check your email for a sign-in link.
				</div>		   		
				<div>		    
				    <input type="text" class="dh-text-input" id="dhSelfInviteAddress${N}" size="30"/>
				</div>    
			    <input type="button" id="dhSelfInviteButton" value="Send" onclick="dhSelfInvite${N}()"/>
			</c:when>
			<c:otherwise>
		        <div class="dh-special-subtitle">
	        		Temporarily out of invitations
        		</div>
        		<div class="dh-special-subtitle dh-landing-explanatory">
        			Sorry, we've run out of invitations to <c:out value="${site.siteName}"/>. If you enter your email address below, we'll let you know when we have more available.
        		</div>
        		<dht:wantsIn buttonText="Take a rain check"/>
			</c:otherwise>
		</c:choose>
	</div>
	<div id="dhSelfInviteMessage${N}" class="dh-landing-result dhInvisible">
		Thinking...
	</div>
</div>
