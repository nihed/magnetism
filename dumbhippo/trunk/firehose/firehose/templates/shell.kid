<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<?python import sitetemplate ?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:py="http://purl.org/kid/ns#" py:extends="sitetemplate">

<head>
    <meta content="text/html; charset=UTF-8" http-equiv="content-type" py:replace="''"/>
    <title>Interactive Shell</title>
    <style type="text/css" media="screen">
@import "${tg.url('/static/css/style.css')}";
</style>
<script src="/static/javascript/jquery-1.2.3.pack.js"></script>
<script type="text/javascript">
shellEval = function() {
  $("#shellstatus").empty();
  $("#shellstatus").text("Executing...");
  $("#shellstatus").show("fast");
  var code = $("#shellinput")[0].value;
  $.ajax({"url": "/shell_exec",
          "type": "POST",
          "contentType": "text/plain",
          "data": code,
          "dataType": "json",
          "success": function(data, status){
            $("#shellresult").empty();
            $("#shellresult").append("Result Type: ");           
            $("#shellresult").append(document.createTextNode(data["type"]));
            $("#shellresult").append(document.createElement("br"));
            $("#shellresult").append("Result Repr: ");
            $("#shellresult").append(document.createTextNode(data["repr"]));
          },
          "error": function(req, status, err) {
            $("#shellresult").empty();
            $("#shellresult").text("Error; status=" + status + " err: " + err);
            },
          "complete": function(req, status) {
            $("#shellstatus").hide("fast");
            },
         });
}
</script>
</head>

<body>
    <div id="main_content">    
    <div id="status_block" class="flash" py:if="value_of('tg_flash', None)" py:content="tg_flash"></div>
    <form name="shell" id="shell" onsubmit="shellEval(); return false;"><textarea id="shellinput" rows="15" cols="90"></textarea><br/><input type="submit" value="Eval"></input></form>
    <div id="shellstatus"></div>
    <div id="shellresult"></div>
    <!-- End of main_content -->
    </div>
<div id="footer"> <img src="${tg.url('/static/images/under_the_hood_blue.png')}" alt="TurboGears under the hood" />
  <p>TurboGears is a open source front-to-back web development
    framework written in Python</p>
  <p>Copyright &copy; 2007 Kevin Dangoor</p>
</div>
</body>

</html>
