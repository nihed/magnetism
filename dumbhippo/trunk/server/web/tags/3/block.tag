<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.BlockView" %>
<%@ attribute name="cssClass" required="true" type="java.lang.String" %>

<div class="dh-stacker-block ${cssClass}">
	<img src="/images3/${buildStamp}/${block.iconName}"/>
	<span class="dh-stacker-block-title">
		<span class="dh-stacker-block-title-type"><c:out value="${block.webTitleType}"/>:</span>
		<span class="dh-stacker-block-title-title">
			<c:choose>
				<c:when test="${!empty block.webTitleLink}">
					<jsp:element name="a">
						<jsp:attribute name="href"><c:out value="${block.webTitleLink}"/></jsp:attribute>
						<jsp:body><c:out value="${block.webTitle}"/></jsp:body>
					</jsp:element>
				</c:when>
				<c:otherwise>
					 <c:out value="${block.webTitle}"/>
				</c:otherwise>
			</c:choose>
		</span>
	</span>
	<div class="dh-stacker-block-right">
		69 minutes ago
	</div>
</div>
