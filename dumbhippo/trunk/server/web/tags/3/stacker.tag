<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="person" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.Integer" %>
<%@ attribute name="stackType" required="true" type="java.lang.String" %>
<%@ attribute name="pageable" required="true" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>

<c:set var="previousBlockType" value="" scope="page"/>

<div class="dh-stacker-container">
	<c:forEach items="${pageable.results}" var="block" varStatus="blockIdx">
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
	<dht:expandablePager pageable="${pageable}" anchor="${stackType}"/>
</div>
