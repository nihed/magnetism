<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="post"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>

<head>
		<script type="text/javascript">
			if (parent != self) {
				// only look at the base parts of the url
				var pLoc = parent.location.host + parent.location.pathname;
				var sLoc = self.location.host + self.location.pathname;
				if (pLoc == sLoc)
					parent.location.href = self.location.href;
			}
		</script>
        <title><c:out value="${title}"/></title>
        <dht:stylesheets />
</head>

<frameset rows="*,165">
    <frame name="top" src="${framer.post.url}">
    <frame name="bottom" src="framer?postId=${framer.postId}" scrolling="no" noresize bordercolor="#cccccc" marginwidth="0" marginheight="0">
</frameset>
<noframes>
Your browser does not support frames.  <a href="${framer.post.url}">Click here</a> for page.
</noframes>

</html>
