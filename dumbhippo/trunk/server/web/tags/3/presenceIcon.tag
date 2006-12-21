<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="iconForPhoto" required="false" type="java.lang.Boolean" %>

<span class="dh-presence">
    <c:choose>
        <c:when test="${iconForPhoto}"> 
            <dh:png src="${who.onlineIconForPhoto}" style="width: 14px; height: 14px;"/>
        </c:when>
        <c:otherwise>    
	        <dh:png src="${who.onlineIcon}" style="width: 12px; height: 12px;"/>
	    </c:otherwise>
	</c:choose>        
</span>
