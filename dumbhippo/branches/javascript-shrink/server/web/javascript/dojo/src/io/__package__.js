dojo.hostenv.conditionalLoadModule({
	common: ["dojo.io", false, false],
	rhino: ["dojo.io.RhinoIO", false, false],
	browser: [["dojo.io.BrowserIO", false, false], ["dojo.io.cookie", false, false]]
});
dojo.hostenv.moduleLoaded("dojo.io.*");
