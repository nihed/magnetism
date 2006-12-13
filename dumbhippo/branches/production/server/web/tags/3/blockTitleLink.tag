<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.SimpleTitleBlockView" %>
<%@ attribute name="homeStack" required="true" type="java.lang.Boolean" %>
<%@ attribute name="spanClass" required="false" type="java.lang.String" %>
<%@ attribute name="linkClass" required="false" type="java.lang.String" %>

<c:if test="${empty linkClass}">
	<c:set var="linkClass" value="dh-underlined-link" scope="page"/>
</c:if>

<c:if test="${empty spanClass}">
	<c:set var="spanClass" value="dh-stacker-block-title-title" scope="page"/>
</c:if>

<span class="${spanClass}">
	<jsp:element name="a">
		<jsp:attribute name="class">${linkClass}</jsp:attribute>
		<jsp:attribute name="href"><c:out value="${block.link}"/></jsp:attribute>
		<jsp:body>
			<%-- if the updates are viewed outside of the person's own homeStack, we --%>
		    <%-- always want them to be in the third person or neutral tone (not "You have") --%>
		    <c:out value="${homeStack ? block.titleForHome : block.title}"/>
		</jsp:body>
	</jsp:element>
</span>