<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="currentCategory" required="true" type="java.lang.String" %>
<%@ attribute name="linkifyCurrent" required="false" type="java.lang.Boolean" %>

<div id="dhApplicationsCategories">
	<div class="dh-applications-heading">View Category:</div>
	<table class="dh-applications-categories" cellspacing="0" cellpadding="0">
		<tr class="dh-applications-category ${current ? 'dh-applications-current-category' : ''}">
			<c:set var="current" value="${empty currentCategory}"/>
			<td>
				<c:choose>
					<c:when test="${current && !linkifyCurrent}">
	    				All
	    			</c:when>
	    			<c:otherwise>
		    			<a href="/applications">All</a>
	    			</c:otherwise>
   				</c:choose>
			</td>
		</tr>
    	<c:forEach items="${applications.categories}" var="category">
			<c:set var="current" value="${currentCategory == category.category}"/>
    		<tr class="dh-applications-category ${current ? 'dh-applications-current-category' : ''}">
    			<td class="dh-applications-category-name}">
    				<c:choose>
    					<c:when test="${current && !linkifyCurrent}">
		    				<c:out value="${category.category.displayName}"/>
		    			</c:when>
		    			<c:otherwise>
			    			<a href="/applications?category=${category.category.name}">
			    				<c:out value="${category.category.displayName}"/>
			    			</a>
		    			</c:otherwise>
	   				</c:choose>
    			</td>
				<td class="dh-applications-category-bar-outer">	    			
    				<div class="dh-applications-category-bar" style="background-color: ${category.color}; width: ${category.length}px;"/>
    			</td>
			</tr>
    	</c:forEach>
	</table>
</div>

