<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="person" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="stack" required="true" type="java.util.List" %>
<%@ attribute name="stackSize" required="false" type="java.lang.Integer" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.Integer" %>

<c:if test="${stackSize == 0}">
	<c:set var="stackSize" value="5"/>
</c:if>

<c:set var="previousBlockType" value="" scope="page"/>

<div class="dh-stacker-container">
	<c:forEach items="${stack}" end="${stackSize}" var="block" varStatus="blockIdx">
		<c:choose>
			<c:when test="${blockIdx.count % 2 == 0}">
				<dht3:block block="${block}" offset="${previousBlockType == block.blockType}" blockId="${stackOrder}_${blockIdx.count + 1}"/>
			</c:when>
			<c:otherwise>
				<dht3:block block="${block}" offset="${previousBlockType == block.blockType}" blockId="${stackOrder}_${blockIdx.count + 1}"/>
			</c:otherwise>
		</c:choose>
		<c:set var="previousBlockType" value="" scope="page"/>		
		<c:if test="${!blockIdx.last}">
			<div class="dh-stacker-block-bottom-padding">&nbsp;</div>
		</c:if>
	</c:forEach>
	<div class="dh-stacker-more"><a href="history?who=${person.viewPersonPageId}">More</a></div>
</div>
