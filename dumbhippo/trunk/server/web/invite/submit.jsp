<%@ include file="../prelude.jspf" %>

<%@ include file="../beaninit.jspf" %>      
           
<html>
  <head>
    <title>Invitation Sent</title>
  </head>
  <body>          
<%
    out.write("<b>Sending...</b>\n");

    String email = request.getParameter("emailaddr");
    
    // FIXME need to validate email param != null and that it is valid rfc822
	EmailResource res = identitySpider.getEmail(email);
	// FIXME we should get the person from the auth data
    Person inviter = identitySpider.getTheMan();
    Invitation invite = invitationSystem.createGetInvitation(inviter, res);
    pageContext.setAttribute("invitation", invite);
    out.write("<b>Invitation sent OK</b>\n");
%>
    <h2>You've invited "${invitation.invitee.humanReadableString}" to become one with the hippo!</h2>
    <c:url var="invitationURL" value="../web/${invitation.partialAuthURL}"/>
    <a href="${invitationURL}">${invitationURL}</a>

  </body>
 </html>