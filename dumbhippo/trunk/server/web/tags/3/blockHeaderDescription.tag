<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<div class="dh-stacker-block-header-description" id="dhStackerBlockHeaderDescriptionContainer-${blockId}">
	<div id="dhStackerBlockHeaderDescription-${blockId}"><jsp:doBody/></div>
</div>
