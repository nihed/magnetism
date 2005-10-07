<%@ include file="../prelude.jspf" %>

<%@ include file="../beaninit.jspf" %>

%<
  String auth = request.getParameter("authkey");
  Invitation invite = invitationSystem.lookupInvitationByKey(auth);
  // TODO create this page
  if (invite.isViewed()) {
    request.redirect("web/invite/viewedalready.jsp");
  }
  HippoAccount account = invitationSystem.viewInvitation(invite);
  Person invitee = account.getOwner();
  PersonView viewedInvitee = spider.getSystemViewpoint(invitee);
  pageContext.setAttribute("viewedInvitee", viewedInvitee);
  pageContext.setAttribute("invitation", invite);
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
          Iterator it = invite.getInviters();
          while (it.hasNext()) {
            Person inviter = it.next();
            PersonView view = spider.getSystemViewpoint(inviter);
            out.println("<li>" + XMLBuilder.escape(view.getHumanReadableName()) + "</li>");
          }
        %>
     </ul>
  </body>
 </html>