<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="exec" required="true" type="java.lang.String" %>
<%@ attribute name="groupId" required="false" type="java.lang.String" %>
<%@ attribute name="ctrlId" required="true" type="java.lang.String" %>
<%@ attribute name="text" required="true" type="java.lang.String" %>
<%@ attribute name="completedText" required="true" type="java.lang.String" %>

<c:if test="${empty groupId}">
	<c:set var="groupId" value="${ctrlId}"/>
</c:if>

<div class="dh-async-action-control">
	<div>
		<script type="text/javascript">
			dhAction${ctrlId} = function () {
				dh.asyncActionLink.exec('${groupId}', '${ctrlId}', function () { ${exec} })
			}
		</script>
		<a id="dhActionExecLink-${ctrlId}" href="javascript:dhAction${ctrlId}()">${text}</a>
	</div>
	<div id="dhActionWorking-${ctrlId}" class="dh-async-action-link-working" style="display: none;">
		Working...
	</div>
	<div id="dhActionComplete-${ctrlId}" class="dh-async-action-link-complete" style="display: none;">
		${completedText}
	</div>
</div>
