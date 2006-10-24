<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="contact" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.Integer" %>
<%@ attribute name="stackType" required="false" type="java.lang.String" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="blocks" required="false" type="java.util.List" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="width" required="false" type="java.lang.String" %>
<%@ attribute name="floatSide" required="false" type="java.lang.String" %>

<c:if test="${empty shortVersion}">
	<c:set var="shortVersion" value="false"/>
</c:if> 

<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if> 

<dht3:shinyBox color="grey" width="${width}" floatSide="${floatSide}">				
	<dht3:personHeader who="${contact}" linkifyName="${embedVersion}" embedVersion="${embedVersion}" shortVersion="${pageable.position > 0}">
	<c:choose>
		<c:when test="${signin.valid}">
            <c:choose>
        	    <c:when test="${contact.viewOfSelf}">
				    <a href="/account">Edit my Mugshot account</a>        		
        	    </c:when>
        	    <c:otherwise>
	        	    <c:choose>
  			            <c:when test="${contact.viewerIsContact != null}">
  				            <dht:actionLink oneLine="true" href="javascript:dh.actions.removeContact('${contact.viewPersonPageId}')" title="Remove this person from your friends list">Remove from friends</dht:actionLink>
	   	                </c:when>
	        	        <c:otherwise>
					        <dht:actionLink oneLine="true" href="javascript:dh.actions.addContact('${contact.viewPersonPageId}')" title="Add this person to your friends list">Add to friends</dht:actionLink>
				        </c:otherwise>
				    </c:choose>
				    | <a href="/">Invite to a group</a>
			    </c:otherwise>
		    </c:choose>
		</c:when>
		<c:otherwise>
		    <a href="/person?who=${contact.viewPersonPageId}">Mugshot page</a>
		</c:otherwise>
	</c:choose>    		    
	</dht3:personHeader>
	<dht3:stacker stackOrder="${stackOrder}" stackType="${stackType}" pageable="${pageable}" blocks="${blocks}" showFrom="${showFrom}" oneLine="${embedVersion}"/>
</dht3:shinyBox>
