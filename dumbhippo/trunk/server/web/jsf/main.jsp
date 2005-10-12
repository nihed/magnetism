<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
	<head>
	<title>Main</title>
	</head>
	<body>
	<h:form>
		<h3>This is the main page.</h3>

		<!-- TODO: dump a JSF data table here with links associated with the 
               		current viewuser --> 
        <!-- TODO: dump a JSF data table here with people connected to the 
               		current viewuser -->

		<p><h:commandLink action="addclient">
			<h:outputText value="Sign in current client" />
		</h:commandLink></p>

		<p><h:commandLink action="invite">
			<h:outputText value="Invite a user" />
		</h:commandLink></p>

		<p><h:commandLink action="sharelink">
			<h:outputText value="Share a link" />
		</h:commandLink></p>
		</h3>
	</h:form>
	<!-- valid signin: <h:outputText value="#{signin.valid}"/> -->
	<c:if test="${signin.valid}">
	  <div style="font-size: small; color: gray;">
	    Logged in as <b><h:outputText value="#{signin.account.owner.name}"/></b>
	    (<h:outputText value="#{signin.account.owner.id}"/>)
	  </div>
	</c:if>
	</body>
</f:view>
</html>
