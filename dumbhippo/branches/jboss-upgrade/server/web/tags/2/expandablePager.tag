<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="pageable" required="true" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="anchor" required="false" type="java.lang.String" %>

<c:if test="${pageable.pageCount > 1}">
	<div class="dh-more">
	<c:choose>
		<c:when test="${pageable.position == 0}">
			<a href="#" onclick='return dh.actions.switchPage("${pageable.name}","${anchor}",1)'>MORE</a>
			<c:if test="${pageable.bound < 0}">
				 <span class="dh-more-count"> 
				     <a href="#" onclick='return dh.actions.switchPage("${pageable.name}","${anchor}",1)'>
				         (<c:out value="${pageable.totalCount}"/>)
				     </a>
				 </span>    
			 </c:if>
			 </a>
			<a href="#" onclick='return dh.actions.switchPage("${pageable.name}","${anchor}",1)'><img src="/images2/${buildStamp}/arrow_right.gif"/></a>
		</c:when>
		<c:otherwise>
			<dh:pagerLinkList pageable="${pageable}" anchor="${anchor}"/>
		</c:otherwise>
	</c:choose>
	</div>
</c:if>

