// config.js runs before we load dojo.js

var dhDebug = true;
var dhServerUri = "/";
var dhScriptRoot = dhServerUri + "javascript/";
if (dhDebug && document.location.toString().substring(0,5) == "file:") {
	// typical developer setup would use this
	dhServerUri = "http://127.0.0.1:8080/";
	dhScriptRoot = "javascript/";
}
var dhXmlRoot = dhServerUri + "xml/";
var dhXmlRpcRoot = dhServerUri + "xmlrpc/";
var dhTextRoot = dhServerUri + "text/";
var dhPostRoot = dhServerUri + "action/";

// configure Dojo; don't put anything here we expect to change, instead make a 
// dh variable above then slave djConfig to it.
var djConfig = { 
	isDebug: dhDebug,
	preventBackButtonFix: true,
//  this loads browser_debug.js which makes module loading a no-op?
//	debugAtAllCosts: true,
	baseScriptUri: dhScriptRoot + "dojo/"
};
