<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="a" required="false" type="java.lang.String" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="query" required="false" type="java.lang.String" %>

<c:if test="${a != null}"><a name="${a}"></a></c:if>
<div> 
	<span <c:if test="${a != null}">id="${a}"</c:if> class="dh-search-title"><jsp:doBody/></span>
	<c:if test="${!empty pageable && pageable.resultCount > 0}">
		<span class="dh-search-info">
			<c:if test="${pageable.pageCount > 1}">
				<c:out value="${pageable.start + 1}"/> to <c:out value="${pageable.currentItemCount}"/> of 
				<c:if test="${pageable.totalCount > pageable.currentItemCount}">about </c:if>
			</c:if>
			<c:out value="${pageable.totalCount}"/> results for
			<span class="dh-search-keyword"><c:out value="${query}"/></span>
		</span>
	</c:if>
</div>
