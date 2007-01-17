<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="prefix" required="true" type="java.lang.String" %>
<%@ attribute name="icon" required="true" type="java.lang.String" %>
<%@ attribute name="text" required="false" type="java.lang.String" %>
<%@ attribute name="tipText" required="true" type="java.lang.String" %>
<%@ attribute name="tipIcon" required="true" type="java.lang.String" %>
<%@ attribute name="tipIconWidth" required="true" type="java.lang.String" %>
<%@ attribute name="tipIconHeight" required="true" type="java.lang.String" %>

<td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/${icon}" style="width: 16; height: 16;"/></td>
<td class="dh-features-list-content-column">
  <div id="dhFeatures${prefix}">
    <c:choose>
      <c:when test="${!empty text}"><c:out value="${text}"/></c:when>
      <c:otherwise><jsp:doBody/></c:otherwise>
    </c:choose>
  </div>
  <div id="dhFeatures${prefix}Tip" class="dh-features-list-tip dh-tooltip">
  	<div class="dh-features-list-tip-content" style="width: ${tipIconWidth}">
      <div><c:out value="${tipText}"/></div>
   	  <div><dh:png src="/images3/${buildStamp}/features_page_samples/${tipIcon}" style="width: ${tipIconWidth}; height: ${tipIconHeight};"/></div>
   </div>
  </div>
    <script type="text/javascript">
	  var tip = new dh.tooltip.Tooltip(document.getElementById("dhPageOuter"), 
	                                   document.getElementById("dhFeatures${prefix}"),
	                                   document.getElementById("dhFeatures${prefix}Tip"));
	  tip.setAlignBottom(true);
	  tip.setCloseOnClick(true);
	  tip.setCloseOnSourceOut(true);
  </script>			    
</td>