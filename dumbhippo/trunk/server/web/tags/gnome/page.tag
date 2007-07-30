<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<div id="gnomePage">
	<gnome:header/>
	<div id="gnomeContent">
		<jsp:doBody/>
		<%-- in case body has floats in it, and to give us some space before the 
			 hrule at bottom of page --%>
		<div style="clear: both; width: 100px; height: 10px;"></div>
	</div>
</div>
