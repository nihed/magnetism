<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="color" required="true" type="java.lang.String" %>
<%@ attribute name="width" required="false" type="java.lang.String" %>
<%@ attribute name="floatSide" required="false" type="java.lang.String" %>

<c:if test="${empty width}">
	<c:set var="width" value="100%"/>
</c:if>

<c:choose>
    <c:when test="${empty floatSide}">
	    <c:set var="floatClass" value=""/>
    </c:when>
    <c:otherwise>
        <c:set var="floatClass" value="dh-shiny-box-${floatSide}"/>
    </c:otherwise>    
</c:choose>

<div class="dh-shiny-box dh-shiny-box-box ${floatClass}" style="width: ${width}">
<div class="dh-shiny-box-box dh-shiny-box-inner dh-shiny-box-inner-${color}">
	<div class="dh-shiny-box-box dh-shiny-box-top dh-shiny-box-top-${color}">
	<div class="dh-shiny-box-box dh-shiny-box-upper-left dh-shiny-box-upper-left-${color}">
	<div class="dh-shiny-box-box dh-shiny-box-upper-right dh-shiny-box-upper-right-${color}">
		<div class="dh-shiny-box-box dh-shiny-box-content-border">
		<div class="dh-shiny-box-box dh-shiny-box-content">
		<table class="dh-shiny-box-box" cellspacing="0" cellpadding="0">
		<tr>
		<td class="dh-shiny-box-content-side-spacer">&nbsp;</td>
		<td><jsp:doBody/></td>
		<td class="dh-shiny-box-content-side-spacer">&nbsp;</td>
		</tr>
		</table>		
	    </div>	
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
