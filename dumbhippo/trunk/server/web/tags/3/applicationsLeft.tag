<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="currentCategory" required="true" type="java.lang.String" %>
<%@ attribute name="linkifyCurrent" required="false" type="java.lang.Boolean" %>

<c:set var="applicationsEnabled" value="${signin.valid && signin.user.account.applicationUsageEnabledWithDefault}"/>
<td id="dhApplicationsLeft">
	<dht3:applicationCategories currentCategory="${currentCategory}" linkifyCurrent="${linkifyCurrent}"/>
	<div id="dhMyApplications" style="${applicationsEnabled ? '' : 'display: none;'}">
		<div class="dh-applications-subheading">Your Application Usage</div>
		<c:choose>
			<c:when test="${applicationsEnabled && applications.myApplications.resultCount > 0}">
				<dht3:miniApplicationList apps="${applications.myApplications}"/>
			</c:when>
			<c:otherwise>
				<div class="dh-applications-no-data">
					Actively monitoring<br/>
					your system...
				</div>
			</c:otherwise>
		</c:choose>
	</div>
</td>
