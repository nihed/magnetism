<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="postId"/>

<c:set var="title" value="${framer.postInfo.title}" scope="page"/>

<frameset rows="100,*">
    <frame name="top" src="/jsp/framer.jsp?postId=${framer.postId}">
    <frame name="bottom" src="${framer.postInfo.url}">
</frameset>
<noframes>
Your browser does not support frames.  <a href="${framer.postInfo.url}">Click here</a> for page.
</noframes>

</html>
