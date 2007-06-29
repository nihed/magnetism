dojo.require("dojo.string");

// NOTE: these tests are mostly a port from test_string.html

function test_string_trim(){
	var ws = " This has some white space at the ends! Oh no!    ";
	var trimmed = "This has some white space at the ends! Oh no!";
	jum.assertEquals("test10", trimmed, dojo.string.trim(ws));
}

function test_string_paramString(){
	var ps = "This %{string} has %{parameters} %{toReplace}";
	var ps1 = dojo.string.paramString(ps, { string: "area", parameters: "foo"});
	jum.assertEquals("test20", "This area has foo %{toReplace}", ps1);

	var ps2 = dojo.string.paramString(ps, { string: "area", parameters: "foo"}, true);
	jum.assertEquals("test30", "This area has foo ", ps2);
}

function test_string_isBlank(){
	jum.assertTrue("test40", dojo.string.isBlank('   '));
	jum.assertFalse("test50", dojo.string.isBlank('            x'));
	jum.assertFalse("test60", dojo.string.isBlank('x             '));
	jum.assertTrue("test70", dojo.string.isBlank(''));
	jum.assertTrue("test80", dojo.string.isBlank(null));
	jum.assertTrue("test90", dojo.string.isBlank(new Array()));
}

function test_string_capitalize(){
	jum.assertEquals("test100", 'This Is A Bunch Of Words', dojo.string.capitalize('this is a bunch of words'));
	jum.assertEquals("test110", 'Word', dojo.string.capitalize('word'));
	jum.assertEquals("test120", '   ', dojo.string.capitalize('   '));
	jum.assertEquals("test130", '', dojo.string.capitalize(''));
	jum.assertEquals("test140", '', dojo.string.capitalize(null));
	jum.assertEquals("test150", '', dojo.string.capitalize(new Array()));
}
