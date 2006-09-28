<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="color" required="true" type="java.lang.String" %>

<div class="dh-shiny-box dh-shiny-box-${color}">
<div class="dh-shiny-box-inner dh-shiny-box-inner-${color}">
	<div class="dh-shiny-box-top dh-shiny-box-top-${color}">
	<div class="dh-shiny-box-upper-left dh-shiny-box-upper-left-${color}">
	<div class="dh-shiny-box-upper-right dh-shiny-box-upper-right-${color}">
		<div class="dh-shiny-box-content">
		<jsp:doBody/>	
	    </div>	
	</div>
	</div>
	</div>	
	<div class="dh-shiny-box-bottom">
	<div class="dh-shiny-box-bottom-left">
	<div class="dh-shiny-box-bottom-right">
	&nbsp;
	</div>
	</div>
	</div>
</div>
</div>
