<html>
   <%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
   <%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>

   <f:view>
      <head>               
         <title>Main</title>
      </head>
      <body>
         <h:form>
            <h3>
               This is the main page.
               
               <p>
               <h:commandLink action="invite">
                 <h:outputText value="Invite a user"/>
               </h:commandLink>
               </p>
               
               <p>
               <h:commandLink action="addlink">
                 <h:outputText value="Share a link"/>
               </h:commandLink>
               </p>
            </h3>
         </h:form>
      </body>      
   </f:view>
</html>