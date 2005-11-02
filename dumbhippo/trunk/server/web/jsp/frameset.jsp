<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="postId"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>

<frameset rows="100,*">
    <frame name="top" src="/jsp/framer.jsp?postId=${framer.postId}">
    <frame name="bottom" src="${framer.post.url}">
</frameset>
<noframes>
Your browser does not support frames.  <a href="${framer.post.url}">Click here</a> for page.
</noframes>

</html>
