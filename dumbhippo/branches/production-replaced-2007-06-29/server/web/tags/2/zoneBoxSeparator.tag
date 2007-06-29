<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%-- some sort of an invisible separator is sometimes necessary at the bottom of a zone --%> 
<%-- in particular, this is the case in artist.jsp when we are displaying an album tag, --%>
<%-- and zone background color comes under the displayed album, unless we have something else, --%>
<%-- like an invisible separator, below the album --%>
<%@ attribute name="visible" required="false" type="java.lang.Boolean"%>

<c:set var="visibleClass" value=""/>

<c:if test="${!empty visible && !visible}">
	<c:set var="visibleClass" value=" dh-separator-invisible"/>
</c:if>

<%-- the div inside here seems to keep IE from ignoring height 1px on the separator --%>
<div class="dh-separator${visibleClass}"><div></div></div>