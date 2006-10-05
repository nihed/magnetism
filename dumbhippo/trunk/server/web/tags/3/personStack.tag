<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="contact" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="stack" required="true" type="java.util.List" %>

<dht3:shinyBox color="grey">				
	<dht3:personHeader who="${contact}" isSelf="false">
        <c:choose>
  		        <c:when test="${contact.contact != null}">
  			        <dht:actionLink oneLine="true" href="javascript:dh.actions.removeContact('${contact.viewPersonPageId}')" title="Remove this person from your friends list">Remove from friends</dht:actionLink>
   	        </c:when>
	        <c:otherwise>
				<dht:actionLink oneLine="true" href="javascript:dh.actions.addContact('${contact.viewPersonPageId}')" title="Add this person to your friends list">Add to friends</dht:actionLink>
			</c:otherwise>
		</c:choose>	| <a href="/">Invite to a group</a>
	</dht3:personHeader>
	<dht3:stacker stack="${stack}"/>
</dht3:shinyBox>
