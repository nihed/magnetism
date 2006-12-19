<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>

<div class="dh-person-item">
    <div class="dh-image">
	    <dht:headshot person="${who}" size="60"/>
    </div>
    <div class="dh-person-item-name">
        <c:if test="${who.liveUser != null}">
            <a href="${who.homeUrl}">
       </c:if>
       <c:out value="${who.truncatedName}"/>
       <c:if test="${who.liveUser != null}">       
           </a>
       </c:if>    
    </div>
</div>    
    