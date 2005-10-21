<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
	<head>
	<title>Blog view</title>
	</head>
	<body>
	<h:dataTable value="#{personpostview.postUrls}" var="post">
	  <h:column>
	    <h:outputText value="#{post}"/>
	  </h:column>
	</h:dataTable>
	</body>
</f:view>
</html>
