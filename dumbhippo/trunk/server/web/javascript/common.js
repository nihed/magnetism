// common.js - adds requires on the dojo bootstrap files
// our dh:script tag forces it to load on all pages that use any javascript

dojo.provide('common');

// these modules are invented in the jscompress script.
// bootstrap1 has to be included already, making it work 
// like a module was too annoying because it defines global
// variables
dojo.require('dojo.hostenv_browser');
dojo.require('dojo.bootstrap2');

// Now provide a bunch of modules that dojo does not 
// explicitly create, as a workaround. dojo does provide
// sub-modules of these, just not the parent modules.
dojo.provide('dojo.widget');
dojo.provide('dojo.widgets');
dojo.provide('dojo.webui');
dojo.provide('dojo.webui.widgets');
dojo.provide('dojo.collections');
dojo.provide('dojo.text');
dojo.provide('dojo.dnd');
dojo.provide('dojo.xml');
dojo.provide('dojo.fx');
dojo.provide('dojo.undo');
dojo.provide('dojo.alg');
dojo.provide('dojo.logging');
dojo.provide('dojo.widget.html');

// This is to catch bugs
dojo.require = function(module) {
	throw new Error("dojo.require should not still exist at runtime, jscompress should have replaced it: " + module);
}
dojo.provide = function(module) {
	throw new Error("dojo.provide should not still exist at runtime, jscompress should have replaced it: " + module);
}
