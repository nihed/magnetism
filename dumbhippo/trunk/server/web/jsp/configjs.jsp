<%-- the contentType breaks the Eclipse jsp editor,
     you need to do Open With, Text Editor instead --%>
<%@ page pageEncoding="UTF-8" contentType="text/javascript"%>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

// config.js runs before we load dojo.js

// Rather than turning this on globally, it's probably better to
// add 'djConfig.isDebug = true' to the page your are editing 
// immediately after including config.js
var dhDebug = false;
var dhServerUri = "/";
var dhScriptRoot = dhServerUri + "javascript/${buildStamp}/";
var dhImageRoot = dhServerUri + "images/${buildStamp}/";
var dhImageRoot2 = dhServerUri + "images2/${buildStamp}/";
var dhImageRoot3 = dhServerUri + "images3/${buildStamp}/";
if (dhDebug && document.location.toString().substring(0,5) == "file:") {
	// typical developer setup would use this
	dhServerUri = "http://127.0.0.1:8080/";
	dhScriptRoot = "javascript/";
}
var dhXmlRoot = dhServerUri + "xml/";
var dhXmlRpcRoot = dhServerUri + "xmlrpc/";
var dhTextRoot = dhServerUri + "text/";
var dhPostRoot = dhServerUri + "action/";
var dhUploadRoot = dhServerUri + "upload/";
var dhFilesRoot = dhServerUri + "files/";

var dhBaseUrl = <dh:jsString value="${baseUrl}"/>;

// configure Dojo; don't put anything here we expect to change, instead make a 
// dh variable above then slave djConfig to it.
var djConfig = { 
	isDebug: dhDebug,
	preventBackButtonFix: true,
//  this loads browser_debug.js which makes module loading a no-op?
//	debugAtAllCosts: true,
	baseScriptUri: dhScriptRoot + "dojo/",
	bindEncoding: "UTF-8" // For post submission. Default is mangled ASCII
};
