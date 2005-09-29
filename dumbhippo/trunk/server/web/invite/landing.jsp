<%@ include file="../prelude.jspf" %>
<html>
  <head>
    <title>Dumb Hippo Invitation for ${invitation.invitee.humanReadableString}</title>
  </head> 
  <body>
    <p>
    Welcome "${invitation.invitee.humanReadableString}" !
    </p>
    <p>
      You were invited by:
    </p>
    <ul>
      <c:forEach var="invite" items="${invitation.inviters}">
        <li>${invite.systemView.humanReadableName}</li>
      </c:forEach>
     </ul>
  </body>
 </html>