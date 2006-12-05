<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="person" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.Integer" %>
<%@ attribute name="stackType" required="false" type="java.lang.String" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="blocks" required="false" type="java.util.List" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>
<%@ attribute name="shortVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="width" required="false" type="java.lang.String" %>
<%@ attribute name="floatSide" required="false" type="java.lang.String" %>
<%@ attribute name="homeStack" required="false" type="java.lang.Boolean" %>

<c:if test="${empty shortVersion}">
	<c:set var="shortVersion" value="false"/>
</c:if> 

<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if> 

<dht3:shinyBox color="grey" width="${width}" floatSide="${floatSide}">				
	<dht3:personHeader who="${person}" linkifyName="${embedVersion}" embedVersion="${embedVersion}" shortVersion="${shortVersion}">
	<c:choose>
		<c:when test="${signin.valid}">
            <c:choose>
        	    <c:when test="${person.viewOfSelf}">
        	    	<div>
					    <a href="/account">Edit my Mugshot account</a>        		
					</div>
        	    </c:when>
        	    <c:otherwise>
	        	    <c:choose>
  			            <c:when test="${person.contactOfViewer}">
  			            	<dh:script module="dh.actions"/>
  				            <dht:actionLink oneLine="true" href="javascript:dh.actions.removeContact('${person.viewPersonPageId}')" title="Remove this person from your friends list">Remove from friends</dht:actionLink>
	   	                </c:when>
	        	        <c:otherwise>
	        	        	<dh:script module="dh.actions"/>
					        <dht:actionLink oneLine="true" href="javascript:dh.actions.addContact('${person.viewPersonPageId}')" title="Add this person to your friends list">Add to friends</dht:actionLink>
				        </c:otherwise>
				    </c:choose>
				    <%-- Not implemented yet %>
				    <%--| <a href="/invitation?who=${group.viewedGroupId}">Invite to a group</a> --%>
			    </c:otherwise>
		    </c:choose>
		</c:when>
		<c:otherwise>
		    <a href="${person.homeUrl}">Mugshot page</a>
		</c:otherwise>
	</c:choose>    		    
	</dht3:personHeader>
	<c:if test="${!shortVersion}">
	    <dht3:stacker stackOrder="${stackOrder}" stackType="${stackType}" pageable="${pageable}" blocks="${blocks}" showFrom="${showFrom}" oneLine="${embedVersion}" homeStack="${homeStack}"/>
    </c:if>
</dht3:shinyBox>
