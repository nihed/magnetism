This is a "black box" test suite for the site.

the "readonly" package has tests that just spider and avoid POST; these tests
don't do a whole lot unless the site is already seeded with data.

the "destructive" package has tests that create and delete stuff.

For load testing we probably want a new "load" package; HttpUnit is useful, but
most of the tests assume the database doesn't change out from under them, 
so we can't just use our normal tests for load testing.

Tests for a particular relative url are named after it, e.g. 
/home = Home.java

Tests that load an url with various parameters are named like HomeAll.java
(loads Home for each user)

To test a page both readonly and destructively, subclassing the test in the 
destructive package is one convention.

The tests in each package are partially ordered, using the @OrderAfter annotation.
The tests in the destructive package build on each other, adding more users and 
then doing stuff with them.

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

There's a cron job on devel.dumbhippo.com that runs this thing
periodically.
