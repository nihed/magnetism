<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.TitleBlockView" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>
<%@ attribute name="homeStack" required="true" type="java.lang.Boolean" %>
<%@ attribute name="spanClass" required="false" type="java.lang.String" %>
<%@ attribute name="linkClass" required="false" type="java.lang.String" %>

<dht3:blockTitle>
    <c:if test="${!oneLine}"> 
	    <span class="dh-stacker-block-title-type"><c:out value="${block.typeTitle}"/>:</span>
	</c:if>
	<dht3:blockPublicIcon block="${block}"/>
	<dht3:blockTitleLink spanClass="${spanClass}" linkClass="${linkClass}" homeStack="${homeStack}" block="${block}"/>
</dht3:blockTitle>
