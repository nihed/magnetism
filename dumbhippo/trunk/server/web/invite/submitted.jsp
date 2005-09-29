<%@ include file="../prelude.jspf" %>

<html>
  <head>
    <title>You've invited "${invitation.invitee.humanReadableName}"</title>
  </head> 
  <body>
    <h2>You've invited "${invitation.invitee.humanReadableName}" to become one with the hippo!</h2>
    <a href="${invitation.authURL}">${invitation.authURL}</a>
  </body>
 </html>