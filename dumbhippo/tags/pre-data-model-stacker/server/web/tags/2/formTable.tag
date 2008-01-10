<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="hasSaveCancelButtons" required="false" type="java.lang.Boolean" %>
<%@ attribute name="tableId" required="false" type="java.lang.String" %>
<%@ attribute name="tableClass" required="false" type="java.lang.String" %>
<%@ attribute name="hasLabelCells" required="false" type="java.lang.Boolean" %>
<%@ attribute name="hasInfoCells" required="false" type="java.lang.Boolean" %>

<c:if test="${empty tableId}">
    <c:set var="tableId" value=""/>
</c:if>

<c:if test="${empty tableClass}">
    <c:set var="tableClass" value=""/>
</c:if>

<%-- it is typical for a table to have label cells, therefore the default is true --%>
<c:if test="${empty hasLabelCells}">
    <c:set var="hasLabelCells" value="true"/>
</c:if>

<%-- it is not typical for a table to have info cells, therefore the default is false --%>
<c:if test="${empty hasInfoCells}">
    <c:set var="hasInfoCells" value="false"/>
</c:if>
    
<div>
	<table id="${tableId}" class="dh-form-table ${tableClass}" cellspacing="0" cellpadding="0">
		<thead>
			<tr>
			    <c:choose>
			        <c:when test="${hasLabelCells}">
				        <th class="dh-label-cell"></th>
				    </c:when>   
					<c:otherwise>
	                    <th width="1px"></th>
	                </c:otherwise> 
	            </c:choose>    
				<c:choose>
				    <c:when test="${hasInfoCells}">
				        <th class="dh-control-cell dh-control-cell-next-to-info"></th>
				        <th class="dh-info-cell"></th>
				    </c:when>
				    <c:otherwise>
				        <th colSpan="2" class="dh-control-cell"></th>
				    </c:otherwise>
				</c:choose>    
				<th class="dh-box-spacer"></th>  
			</tr>
		</thead>
		<tbody>
			<jsp:doBody/>
		</tbody>
	</table>
	<c:if test="${hasSaveCancelButtons}">
		<div>
			<input type="button" value="Save Changes"/> <input type="button" value="Cancel Changes"/>
		</div>
	</c:if>
</div>
