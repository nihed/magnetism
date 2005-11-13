This is a "black box" test suite for the site.

the "readonly" package has tests that just spider and avoid POST; these tests
don't do a whole lot unless the site is already seeded with data.

the "destructive" package has tests that create and delete stuff.

The tests use HttpUnit and a convenience library on top of it called
jWebUnit.

http://httpunit.sourceforge.net/
http://jwebunit.sourceforge.net/

http://www.codeczar.com/technologies/httpunit/HttpUnitHowTo.html
has some good discussion of how to organize tests, though the
details don't really apply since we're using jwebunit which keeps
global state of the "conversation" in a WebTester object.

See com.dumbhippo.hungry.example for a simple example of how
our test stuff works. The thing to note is that one test 
can create another test by passing in a WebTester, and 
then use methods from the other test.

