<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="hasSaveCancelButtons" required="false" type="java.lang.Boolean" %>
<%@ attribute name="bodyId" required="false" type="java.lang.String" %>

<c:if test="${empty bodyId}">
    <c:set var="bodyId" value=""/>
</c:if>

<div>
	<table class="dh-form-table" cellspacing="0" cellpadding="0">
		<thead>
			<tr>
				<th></th><th></th>
			</tr>
		</thead>
		<tbody id="${bodyId}">
			<jsp:doBody/>
		</tbody>
	</table>
	<c:if test="${hasSaveCancelButtons}">
		<div>
			<input type="button" value="Save Changes"/> <input type="button" value="Cancel Changes"/>
		</div>
	</c:if>
</div>
