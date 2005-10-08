<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>

<f:view>
	<head>
	<title>Verify</title>
	</head>
	<body>
	<h:form>
		<h3>verify page</h3>
		<p>This is the verify page for <h:outputText value="#{verify.authKey}"/></p>
		<p>You were invited by:</p>
		<h:dataTable value="#{verify.inviterNames}" var="name">
		  <h:column>
            <h:outputText value="#{name}" />
          </h:column>
        </h:dataTable>
		<p><h:commandButton value="Go back to the main page" action="mainpage" /></p>
	</h:form>
	</body>
</f:view>
</html>
