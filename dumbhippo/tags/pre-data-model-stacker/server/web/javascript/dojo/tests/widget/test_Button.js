dojo.require("dojo.widget.Button");

function test_button_ctor(){
	var b1 = new dojo.widget.Button();

	jum.assertTrue("test10", typeof b1 == "object");
	jum.assertTrue("test20", b1.widgetType == "Button");
	jum.assertTrue("test21", typeof b1["attachProperty"] == "undefined");
}
