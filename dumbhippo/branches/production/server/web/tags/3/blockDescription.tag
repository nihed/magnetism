<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="literalBody" required="false" type="java.lang.Boolean" %>
<%@ attribute name="hideOnExpand" required="false" type="java.lang.Boolean" %>

<div class="dh-stacker-block-header-description" id="dhStackerBlockHeaderDescriptionContainer-${blockId}">
	<c:choose>
		<c:when test="${!literalBody}">
			<div id="dhStackerBlockHeaderDescription-${blockId}" class="dh-stacker-block-header-description-summary"></div>
			<div id="dhStackerBlockDescription-${blockId}" class="dh-stacker-block-header-description-full"><jsp:doBody/></div>
			<dh:script module="dh.stacker"/>
			<script type="text/javascript">
				dh.stacker.insertBlockHeaderDescription("${blockId}");
			</script>
		</c:when>
		<c:otherwise>
			<div id="dhStackerBlockDescription-${blockId}" class="dh-stacker-block-header-description-full"><jsp:doBody/></div>
			<script type="text/javascript">
				var div = document.getElementById("dhStackerBlockDescription-${blockId}");
				div.dhHideOnExpand = <c:out value="${hideOnExpand ? 'true' : 'false'}"/>;
			</script>
		</c:otherwise>
	</c:choose>
</div>
