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
		<h:outputText rendered="#{!verify.valid}" value="Invalid invitation" style="color: #FF0000"/>
		<h:outputText rendered="#{verify.valid}" value="You were invited by:" style="display: block"/>
		<!-- begin table -->
		<h:dataTable rendered="#{verify.valid}" value="#{verify.inviterNames}" var="name">
		  <h:column>
            <h:outputText style="font-weight: bold" value="#{name}" />
          </h:column>
        </h:dataTable>
        <!-- end table -->
		<p><h:commandButton value="Go back to the main page" action="mainpage" /></p>
	</h:form>
	</body>
</f:view>
</html>
