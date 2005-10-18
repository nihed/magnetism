// config.js runs before we load dojo.js

// configure Dojo
var djConfig = { 
	isDebug: true,
	preventBackButtonFix: true,
//  this loads browser_debug.js which makes module loading a no-op?
//	debugAtAllCosts: true,
	baseScriptUri: "javascript/dojo/"
};

var dhServerUri = "http://127.0.0.1:8080/"
var dhXmlRoot = dhServerUri + "xml/";
var dhXmlRpcRoot = dhServerUri + "xmlrpc/";
var dhTextRoot = dhServerUri + "text/";
