<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="user" required="true" type="com.dumbhippo.server.PersonView"%>
<c:out value="${user.liveUser.guid}"/> (<c:out value="${user.name}"/>)  
hotness: <c:out value="${user.liveUser.hotness}"/>,
avail count: <c:out value="${user.liveUser.availableCount}"/>,
hot posts: <c:forEach items="${user.liveUser.activePosts}" var="guid"><c:out value="${guid}"/> </c:forEach>
<br/>