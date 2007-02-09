<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="framer" class="com.dumbhippo.web.pages.FramerPage" scope="request"/>
<jsp:setProperty name="framer" property="isVisit" value="true"/>
<jsp:setProperty name="framer" property="postId" param="post"/>

<dht3:validateFramer page="visit" framer="${framer}"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>
<head>
	<dht:embedObject/>
	<script type="text/javascript">
		var escapedTitle = <dh:jsString value="${dh:xmlEscape(framer.post.title)}"/>;
		var escapedUrl = <dh:jsString value="${dh:xmlEscape(framer.post.url)}"/>;	
		function dhWriteFramesPage() {
			document.open();
			document.write(
"<html>\n" +
"<head>\n" +
"   <title>" + escapedTitle + "</title>\n" +
"	<script type='text/javascript'>\n" +
"		if (parent != self) {\n" +
"			// only look at the base parts of the url\n" +
"			var pLoc = parent.location.host + parent.location.pathname;\n" +
"			var sLoc = self.location.host + self.location.pathname;\n" +
"			if (pLoc == sLoc)\n" +
"				parent.location.href = self.location.href;\n" +
"		}\n" +
"	<" + "/script>\n" +
"</head>\n" +
"<frameset rows='*,125'>\n" +
"   <frame name='top' src='" + escapedUrl + "'>\n" +
"    <frame name='bottom' src='framer?postId=${framer.postId}' scrolling='no' noresize bordercolor='#cccccc' marginwidth='0' marginheight='0'>\n" +
"</frameset>\n" +
"<noframes>\n" + 
"    Your browser does not support frames.  <a href='" + escapedUrl + "'>Click here</a> for page.\n" +
"</noframes>\n" +
"</html>\n"
			);
			document.close();
		}
		function dhInit() {
			var embedObject = document.getElementById("dhEmbedObject");
	        if (embedObject && embedObject.readyState && embedObject.readyState == 4) {
				embedObject.OpenBrowserBar();
  		        window.open(<dh:jsString value="${framer.post.url}"/>, "_self", "", true);
			} else {
				dhWriteFramesPage();
			}
		}
	</script>
    <title><c:out value="${framer.post.title}"/></title>
    <dht:stylesheets />
</head>
<body onload="dhInit()">
<dht3:analytics/>
</body>
</html>
