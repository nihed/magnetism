<%@ include file="../prelude.jspf" %>

<%@ include file="../beaninit.jspf" %>

<%
  String auth = request.getParameter("auth");
  Invitation invite = invitationSystem.lookupInvitationByKey(auth);
  if (invite == null) {
     out.println("<h1>invalid invitation</h1>");
  } else {
  // TODO create this page
  if (invite.isViewed()) {
    out.println("<h1>invitation already viewed</h1>");
  }
  HippoAccount account = invitationSystem.viewInvitation(invite);
  Person invitee = account.getOwner();
  PersonView viewedInvitee = identitySpider.getSystemViewpoint(invitee);
  pageContext.setAttribute("viewedInvitee", viewedInvitee);
  pageContext.setAttribute("invitation", invite);
  }
%>

<html>
  <head>
    <title>Dumb Hippo Invitation for ${viewedInvitee.humanReadableString}</title>
  </head> 
  <body>
    <p>
    Welcome "${viewedInvitee.humanReadableString}" !
    </p>
    <p>
      You were invited by:
    </p>
    <ul>
      <c:forEach var="name" items="${invitationSystem.inviterNames}">
        <li>${name}</li>
      </c:forEach>
     </ul>
  </body>
 </html>