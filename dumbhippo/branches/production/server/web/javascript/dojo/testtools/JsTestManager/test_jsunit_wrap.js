if(this["load"]){
	load(["jsunit_wrap.js"]);
	bu_alert = print;
}

function test_JsUnitWrap_simple() {
  jum.assertEquals('test1', 1, 1);
  jum.assertTrue('test2', true);
  jum.assertTrue('test3', "true");
  jum.assertFalse('test4', false);
}

function test_JsUnitWrap_deep() {
  if (!jum_uneval) {jum.untested("JsUnitWrap_deep"); return;}
  var a = [1,2];
  var b = [1,2];
  jum.assertEquals('test1', a, b);
}

function test_JsUnitWrap_continueAsync() {
  jum.continueAsync('JsUnitWrap_continueAsync', null, 'testing continueAsync');
  jum.resume('JsUnitWrap_continueAsync', null, function() {jum.assertTrue('tada', true)})
}

function test_JsUnitWrap_waitAll() {
  jum.waitAll('JsUnitWrap_waitAll', arguments.callee);
}

jum.init();
