<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<div id="dhApplicationsLeft">
	<dht3:applicationCategories currentCategory="${appView.application.category}" linkifyCurrent="true"/>
	<c:if test="${signin.valid && signin.user.account.applicationUsageEnabledWithDefault}">
		<div class="dh-applications-subheading">Your Application Usage</div>
		<dht3:miniApplicationList apps="${applications.myApplications}"/></div>
	</c:if>
</div>
