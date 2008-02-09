<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<%@ attribute name="currentPageLink" required="true" type="java.lang.String" %>

<div id="gnomePage">
	<gnome:header currentPageLink="${currentPageLink}"/>
	<div id="gnomeContent">
		<jsp:doBody/>
		<%-- in case body has floats in it, and to give us some space before the 
			 hrule at bottom of page --%> 
		<div style="clear: both; width: 100px; height: 10px;"></div>
	</div>
	<div class="gnome-learn-more">
        <a href="http://live.gnome.org/OnlineDesktop" target="_blank">Learn about GNOME Online</a>
    </div>    
</div>
