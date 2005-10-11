<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Fooling with js</title>
<script src="/dumbhippo/javascripts/prototype.js" type="text/javascript"></script>
<script src="/dumbhippo/javascripts/scriptaculous.js" type="text/javascript"></script>
</head>
<body>

<p><a href="/dumbhippo/javascripts/prototype.js">prototype.js</a></p>
<p><a href="/dumbhippo/javascripts/scriptaculous.js">scriptaculous.js</a></p>

<style type="text/css">
    div.auto_complete {
      position:absolute;
      width:250px;
      background-color:white;
      border:1px solid #888;
      margin:0px;
      padding:0px;
    }
    li.selected { background-color: #ffb; }
</style>

<input type="text" id="autocomplete"/><div id="autocomplete_choices"></div>

<script type="text/javascript" language="javascript">
// <![CDATA[
new Ajax.Autocompleter("autocomplete", "autocomplete_choices", "/dumbhippo/xml/people", {method: 'get', asynchronous: true});
// ]]>
</script>

</body>
</html>
