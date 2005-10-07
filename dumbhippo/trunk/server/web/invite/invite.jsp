<%@ include file="../prelude.jspf" %>

<html>
  <head>
    <title>invite a friend to become one with the hippo!</title>
  </head> 
  <body>
    <h2>invite a friend to become one with the hippo!</h2>
    <c:url var="emailResult" value="/web/invite/submit"/>
    <form method="POST" action="${emailResult}">    
    <table>
    <tr>
      <td>Your email</td>
      <td>
        <input id="inviterEmail" type="text" name="inviterEmail"></input>
      </td>
    </tr>    
    <tr>
      <td>Email</td>
      <td>
        <input id="emailaddr" type="text" name="emailaddr"></input>
      </td>
    </tr>
    <tr>
      <td><input type="submit" value="Send"></input></td>
    </tr>
    </table>
    </form>    
  </body>
 </html>