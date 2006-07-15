<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.live.LivePost"%>
<c:out value="${post.guid}"/>  score: <c:out value="${post.score}"/> 
<c:out value="${post.recentMessageCount}"/> recent msgs,
<c:out value="${post.chattingUserCount}"/> people chatting,
<c:out value="${post.viewingUserCount}"/> people viewing
<br/>