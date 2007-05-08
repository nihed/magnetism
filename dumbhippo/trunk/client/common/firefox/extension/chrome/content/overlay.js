// This whole file is reloaded and object recreated for every toplevel window (but not tab!)

var gHippoWindowContext = new Hippo.WindowContext();

window.addEventListener("load", function(e) { gHippoWindowContext.onLoad(e); }, false)
window.addEventListener("unload", function(e) { gHippoWindowContext.onUnload(e); }, false)
