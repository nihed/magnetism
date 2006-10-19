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

<c:set var="previousBlockType" value="" scope="page"/>

<c:if test="${!empty pageable}">
	<c:set var="blocks" value="${pageable.results}" scope="page"/>
</c:if>

<div class="dh-stacker-container">
	<c:forEach items="${blocks}" var="block" varStatus="blockIdx">
		<c:choose>
			<c:when test="${blockIdx.count % 2 == 0}">
				<dht3:block block="${block}" offset="${previousBlockType == block.blockType}" blockId="${stackOrder}_${blockIdx.count + 1}" showFrom="${showFrom}"/>
			</c:when>
			<c:otherwise>
				<dht3:block block="${block}" offset="${previousBlockType == block.blockType}" blockId="${stackOrder}_${blockIdx.count + 1}" showFrom="${showFrom}"/>
			</c:otherwise>
		</c:choose>
		<c:set var="previousBlockType" value="" scope="page"/>		
		<c:if test="${!blockIdx.last}">
			<div class="dh-stacker-block-bottom-padding">&nbsp;</div>
		</c:if>
	</c:forEach>
	<c:if test="${!empty pageable}">
	    <dht:expandablePager pageable="${pageable}" anchor="${stackType}"/>
	</c:if>    
</div>
