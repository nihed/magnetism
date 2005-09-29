<%@ include file="../prelude.jspf" %>

<html>
  <head>
    <title>You've invited "${invitation.invitee.humanReadableString}"</title>
  </head> 
  <body>
    <h2>You've invited "${invitation.invitee.humanReadableString}" to become one with the hippo!</h2>
    <c:url var="invitationURL" value="../web/${invitation.partialAuthURL}"/>
    <a href="${invitationURL}">${invitationURL}</a>
  </body>
 </html>