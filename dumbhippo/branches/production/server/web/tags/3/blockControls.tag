<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="chatHeader" required="false" type="java.lang.Boolean" %>

<div class="dh-stacker-block-controls" id="dhStackerBlockControls-${blockId}${chatHeader ? '-expanded' : ''}" style="${chatHeader ? '' : 'display: none;'}">
	<jsp:doBody/>
</div>
