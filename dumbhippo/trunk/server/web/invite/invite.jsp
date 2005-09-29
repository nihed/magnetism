<%@ include file="../prelude.jspf" %>

<html>
  <head>
    <title>invite a friend to become one with the hippo!</title>
  </head> 
  <body>
    <h2>invite a friend to become one with the hippo!</h2>
    <c:url var="emailResult" value="/actions/invite"/>        
    <form method="POST" action="${emailResult}">    
    <table>
    <tr>
      <td>Email</td>
      <td>
        <input id="emailaddr" name="emailaddr"></input>
      </td>
    </tr>
    <tr>
      <td><input type="submit" value="Send"></input></td>
    </tr>
    </table>
    </form>    
  </body>
 </html>