<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="id" required="true" type="java.lang.String" %>	
<%@ attribute name="extraClass" required="false" type="java.lang.String" %>	

<div id="${id}" class="dh-popup ${extraClass}" style="display: none;">
	<div class="dh-border">
		<div class="dh-content-padding">
			<div class="dh-content">
				<jsp:doBody/>
			</div>
		</div>
	</div>
</div>
