<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="cssClass" required="true" type="java.lang.String" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="expandable" required="false" type="java.lang.Boolean" %>

<jsp:element name="div">
	<jsp:attribute name="class">dh-stacker-block <c:if test="${expandable}">dh-stacker-block-expandable </c:if>${cssClass}</jsp:attribute>
	<jsp:attribute name="id">dhStackerBlock-${blockId}</jsp:attribute>
	<jsp:attribute name="onclick"><c:if test="${expandable}">dh.stacker.onBlockClick('${blockId}');</c:if></jsp:attribute>	
	<jsp:body>
		<jsp:doBody/>
	</jsp:body>
</jsp:element>
