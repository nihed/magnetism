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

		<p><h:commandLink action="invite">
			<h:outputText value="Invite a user" />
		</h:commandLink></p>

		<c:url value="/sharelink.html" var="sharelinkurl"/>
		<p><a href="${sharelinkurl}"><h:outputText value="Share a link" /></a></p>
		
 		<p>
  		<h:panelGroup rendered="#{signin.valid}">
		    <h:commandLink action="#{signin.doLogout}">
			     <h:outputText value="Log out #{signin.user}"/>
		       </h:commandLink>
   		</h:panelGroup>
        </p>
   
        <p>
   		<h:panelGroup rendered="#{!signin.valid}">
			<h:commandLink action="addclient">
				  <h:outputText value="Sign in current client" />
			   </h:commandLink>
   		</h:panelGroup>
   		</p>
   		
        <p>
   		<h:panelGroup rendered="#{signin.valid}">
			<h:commandLink action="blog">
				<f:param name="personId" value="#{signin.user.id}"/>
				  <h:outputText value="View your blog" />
			   </h:commandLink>
   		</h:panelGroup>
   		</p>   		

		</h3>
   
	</h:form>
	</body>
</f:view>
</html>
