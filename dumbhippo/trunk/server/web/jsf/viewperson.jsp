<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
	<head>
	<title><h:outputText value="#{viewperson.name}"/></title>
	</head>
	<body>
	<div class="person">
		<strong><h:outputText value="#{viewperson.name}"/></strong>
    	<br/>
		<div class="shared-links">	
		<p>Recently posted links:</p>	
		<h:dataTable value="#{viewperson.postUrls}" var="post">
			<h:column>
		    <h:outputText value="#{post}"/>
			</h:column>
		</h:dataTable>
		</div>
		<br/>
		<div class="groups">
		<!--  list group objects here -->
		<!--  ability to join/share groups [D] -->
		</div>
	</div>	
	</body>
</f:view>
</html>
