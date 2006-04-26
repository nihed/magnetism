<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="index" required="true" type="java.lang.Integer" %>
<%@ attribute name="total" required="true" type="java.lang.Integer" %>
<%@ attribute name="pagerQuery" required="true" type="java.lang.String" %>
<%@ attribute name="pagerAnchor" required="true" type="java.lang.String" %>

<c:choose>
	<c:when test="${open}">
		<c:set var="image" value="/images2/arrow_down.gif" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="image" value="/images2/arrow_right.gif" scope="page"/>
	</c:otherwise>
</c:choose>

<script type="text/javascript">
	var dhPagerClosure<c:out value="${pagerQuery}"/> = function(i) {
		dh.links.doPagerIndex("<c:out value="${pagerQuery}"/>", i, "<c:out value="${pagerAnchor}"/>");
		return false;
	}
</script>
<div class="dh-more">
<c:choose>
	<c:when test="${index == 0}">
		<a href="${more}" onclick="return dhPagerClosure<c:out value="${pagerQuery}"/>(1);">MORE</a> (<c:out value="${total}"/>)
		<a href="${more}" onclick="return dhPagerClosure<c:out value="${pagerQuery}"/>(1);"><img src="${image}"/></a></div>
	</c:when>
	<c:otherwise>
		<dh:pagerLinkList resultsPerPage="6" index="${index}" total="${total}" onClick="dhPagerClosure${pagerQuery}"/>
	</c:otherwise>
</c:choose>

