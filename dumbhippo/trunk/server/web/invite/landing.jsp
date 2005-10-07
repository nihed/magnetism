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
        <%
          if (invite != null) {
          Iterator it = invite.getInviters().iterator();
          while (it.hasNext()) {
            Person inviter = (Person) it.next();
            PersonView view = identitySpider.getSystemViewpoint(inviter);
            String readable = view.getHumanReadableName();
            if (readable == null) {
              readable = "(unknown)";
            }
            out.println("<li>" + XMLBuilder.escape(readable) + "</li>");
          }
          }
        %>
     </ul>
  </body>
 </html>