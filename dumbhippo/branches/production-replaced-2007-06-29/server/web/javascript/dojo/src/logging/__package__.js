dojo.hostenv.conditionalLoadModule({
	common: ["dojo.logging.Logger", false, false],
	rhino: ["dojo.logging.RhinoLogger"]
});
dojo.hostenv.moduleLoaded("dojo.logging.*");
