<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<div class="dh-stacker-block-content" id="dhStackerBlockContent-${blockId}">
	<div class="dh-stacker-block-content-main">        
		<jsp:doBody/>
	</div>
</div>