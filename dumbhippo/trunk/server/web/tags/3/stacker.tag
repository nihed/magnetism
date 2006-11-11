<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%-- You can supply blocks to this tag in a form of a pageable and stackType or in a form of a blocks list --%>
<%@ attribute name="stackOrder" required="true" type="java.lang.Integer" %>
<%@ attribute name="stackType" required="false" type="java.lang.String" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="blocks" required="false" type="java.util.List" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="homeStack" required="false" type="java.lang.Boolean" %>

<c:if test="${empty oneLine}">
	<c:set var="oneLine" value="false"/>
</c:if> 

<c:if test="${!empty pageable}">
	<c:set var="blocks" value="${pageable.results}" scope="page"/>
</c:if>

<div class="dh-stacker-container">
	<c:forEach items="${blocks}" var="block" varStatus="blockIdx">
		<dht3:block block="${block}" offset="${blockIdx.count % 2 == 0}" blockId="${stackOrder}_${blockIdx.count + 1}" showFrom="${showFrom}" oneLine="${oneLine}" homeStack="${homeStack}"/>	
		<c:if test="${!blockIdx.last}">
			<div class="dh-stacker-block-bottom-padding">&nbsp;</div>
		</c:if>
	</c:forEach>
	<c:if test="${!empty pageable}">
	    <dht:expandablePager pageable="${pageable}" anchor="${stackType}"/>
	</c:if>    
</div>
