<html>
   <%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
   <%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>

   <f:view>
      <head>               
         <title>Verify</title>
      </head>
      <body>
         <h:form>
            <h3>
               This is the verify page.
               
               This should parse out the authKey from the request parameters and mark
               the user verified or not in the database as appropriate.
            </h3>
            <p>
               <h:commandButton value="Verify" action="submitverify"/>
            </p>
         </h:form>
      </body>      
   </f:view>
</html>