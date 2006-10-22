<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.Integer" %>
<%@ attribute name="stackType" required="false" type="java.lang.String" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="blocks" required="false" type="java.util.List" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>
<%@ attribute name="width" required="false" type="java.lang.String" %>
<%@ attribute name="floatSide" required="false" type="java.lang.String" %>

<dht3:shinyBox color="orange" width="${width}" floatSide="${floatSide}">
    <dht3:groupHeader who="${who}">
        <c:choose>
            <c:when test="${signin.valid}">
	            <%-- FIXME: show group actions here ... need to move stuff from StackedGroupPage to GroupView --%>
	            <jsp:doBody/>
            </c:when>
            <c:otherwise>
		        <a href="${who.homeUrl}">Group page</a>
		    </c:otherwise>
		</c:choose>        
	</dht3:groupHeader>
    <dht3:stacker stackOrder="${stackOrder}" stackType="${stackType}" pageable="${pageable}" blocks="${blocks}" showFrom="${showFrom}"/>    
</dht3:shinyBox>		