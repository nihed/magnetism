djConfig = { 
	baseRelativePath: "../",
	isDebug: true
};

load("../src/bootstrap1.js");
// FIXME: need a way to determine which hostenv to load here!!!
load("../src/hostenv_rhino.js");
load("../src/bootstrap2.js");

// compat fixes for BUFakeDom.js and the JUM implementation:
var bu_alert = (typeof this.alert != 'undefined') ? this.alert : (this.load && this.print ? this.print : function() {});
