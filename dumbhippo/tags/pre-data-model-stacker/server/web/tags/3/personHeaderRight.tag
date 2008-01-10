<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="includeWhereAt" required="true" type="java.lang.Boolean" %>
<%@ attribute name="align" required="true" type="java.lang.String" %>

<td align="right">             
	<table cellpadding="0" cellspacing="0">
	<tbody>
	<c:choose>
		<c:when test="${align == 'middle'}">
			<tr valign="top">		
			<td align="center"><jsp:doBody/></td>
			<c:if test="${includeWhereAt}">
   				<td align="right">
					<dht3:whereAtIcons who="${who}"/>
			    </td>
			</c:if>
			</tr>		    
		</c:when>
		<c:otherwise>
			<tr valign="top">	
			<c:if test="${includeWhereAt}">				
	   			<td align="right">
					<dht3:whereAtIcons who="${who}"/>
			    </td>
			</c:if>
		    </tr>
		    <tr>
    			<td align="right">
					<jsp:doBody/>						            
				</td>		    		
			</tr>				
		</c:otherwise>
	</c:choose>
	</tbody>
	</table>
</td>
