<%--
  -	$RCSfile$
  -	$Revision: 1746 $
  -	$Date: 2005-08-04 17:36:08 -0400 (Thu, 04 Aug 2005) $
  -
  - Copyright (C) 2004 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="java.io.*,
                 java.text.SimpleDateFormat,
                 org.jivesoftware.messenger.user.User,
                 java.util.Date,
                 java.text.ParseException,
                 org.jivesoftware.messenger.auth.UnauthorizedException,
                 org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.util.Log,
                 org.jivesoftware.util.StringUtils"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />

<%!
    static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd kk:mm:ss");

    private static final String parseDate(String input) {
        if (input == null || "".equals(input)) {
            return input;
        }
        if (input.length() < 19) {
            return input;
        }
        String d = input.substring(0,19);
        // try to parse it
        try {
            StringBuffer buf = new StringBuffer(input.length());
            synchronized (formatter) {
                Date date = formatter.parse(d);
                buf.append("<span class=\"date\" title=\"").append(formatter.format(date))
                        .append("\">");
            }
            buf.append(d).append("</span>");
            buf.append(input.substring(19,input.length()));
            return buf.toString();
        }
        catch (ParseException pe) {
            return input;
        }
    }

    private static final String hilite(String input) {
        if (input == null || "".equals(input)) {
            return input;
        }
        if (input.indexOf("org.jivesoftware.") > -1) {
            StringBuffer buf = new StringBuffer();
            buf.append("<span class=\"hilite\">").append(input).append("</span>");
            return buf.toString();
        }
        else if (input.trim().startsWith("---") && input.trim().endsWith("---")) {
            StringBuffer buf = new StringBuffer();
            buf.append("<span class=\"hilite-marker\">").append(input).append("</span>");
            return buf.toString();
        }
        return input;
    }
%>

<%
    // Get parameters
    String log = ParamUtils.getParameter(request,"log");
    String numLinesParam = ParamUtils.getParameter(request,"lines");
    int numLines = ParamUtils.getIntParameter(request,"lines",50);
    String mode = ParamUtils.getParameter(request,"mode");

    // Set defaults
    if (log == null) {
        log = "error";
    }
    if (mode == null) {
        mode = "asc";
    }
    if (numLinesParam == null) {
        numLinesParam = "50";
    }

    // Other vars
    File logDir = new File(Log.getLogDirectory());
    String filename = log + ".log";
    File logFile = new File(logDir, filename);

    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "UTF-8"));
    String line = null;
    int totalNumLines = 0;
    while ((line=in.readLine()) != null) {
        totalNumLines++;
    }
    in.close();
    // adjust the 'numLines' var to match totalNumLines if 'all' was passed in:
    if ("All".equals(numLinesParam)) {
        numLines = totalNumLines;
    }
    String[] lines = new String[numLines];
    in = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "UTF-8"));
    // skip lines
    int start = totalNumLines - numLines;
    if (start < 0) { start = 0; }
    for (int i=0; i<start; i++) {
        in.readLine();
    }
    int i = 0;
    if ("asc".equals(mode)) {
        while ((line=in.readLine()) != null && i<numLines) {
            line = StringUtils.escapeHTMLTags(line);
            line = parseDate(line);
            line = hilite(line);
            lines[i] = line;
            i++;
        }
    }
    else {
        int end = lines.length-1;
        while ((line=in.readLine()) != null && i<numLines) {
            line = StringUtils.escapeHTMLTags(line);
            line = parseDate(line);
            line = hilite(line);
            lines[end-i] = line;
            i++;
        }
    }
    numLines = start + i;
%>

<html>
<head>
    <title><%= log %></title>
    <style type="text/css">
    .log TABLE {
        border : 1px #ccc solid;
    }
    .log TH {
        font-family : verdana, arial;
        font-weight : bold;
        font-size : 8pt;
    }
    .log TR TH {
        background-color : #ddd;
        border-bottom : 1px #ccc solid;
        padding-left : 2px;
        padding-right : 2px;
        text-align : left;
    }
    .log .head-num {
        border-right : 1px #ccc solid;
    }
    .log TD {
        font-family : courier new,monospaced;
        font-size : 9pt;
        background-color : #ffe;
    }
    .log .num {
        width : 1%;
        background-color : #eee !important;
        border-right : 1px #ccc solid;
        padding-left : 2px;
        padding-right : 2px;
    }
    .log .line {
        padding-left : 10px;
    }
    .hilite {
        color : #900;
    }
    .hilite-marker {
        background-color : #ff0;
        color : #000;
        font-weight : bold;
    }
    </style>
</head>
<body>

<div class="log">
<table cellpadding="1" cellspacing="0" border="0" width="100%">
<tr>
    <th class="head-num"><fmt:message key="log.line" /></th>
    <th>&nbsp;</th>
</tr>
<tr>
    <td width="1%" nowrap class="num">
        <%  if ("asc".equals(mode)) { %>
            <%  for (int j=start+1; j<=numLines; j++) { %>
                <%= j %><br>
            <%  } %>
        <%  } else { %>
            <%  for(int j=numLines; j>=start+1; j--) { %>
                <%= j %><br>
            <%  } %>
        <%  } %>
    </td>
    <td width="99%" class="line">
        <%  for (int j=0; j<lines.length; j++) {
                if (lines[j] != null) {
        %>
            <nobr><%= lines[j] %></nobr><br>

        <%      }
            }
        %>
    </td>
</tr>
</table>
</div>

</body>
</html>