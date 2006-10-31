<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="user" required="true" type="com.dumbhippo.server.views.PersonView"%>
<a href="/person?who=${user.liveClientData.guid}">${user.liveClientData.guid}</a> (<c:out value="${user.name}"/>)  
avail count: <c:out value="${user.liveClientData.availableCount}"/>, hotness: <c:out value="${user.liveClientData.hotness}"/>, 
hot posts: <c:forEach items="${user.liveClientData.activePosts}" var="guid"><c:out value="${guid}"/> </c:forEach>
<br/>