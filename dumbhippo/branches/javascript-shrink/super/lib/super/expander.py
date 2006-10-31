# Copyright 2005, Red Hat, Inc.

import re
import sys

class Expander:

    """A class that performs file expansion doing substition
    of parameters (as @@paramname@@) and handling of special
    control statemts (@@if/@@elif/@@else/@@endif blocks
    and @@error). The way the class works is that it is
    an iterator that filters another iterator. Typically
    the source iterator would be a file object."""

    def __init__(self, scope, source, display_path):
        """Create a new Expander object

        scope -- the object for doing param lookups (Config or Service)
        source -- the source iterator, typically a file
        display_path -- how to display the source in error messages
        """
        self.display_path = display_path
        self.source = source
        self.scope = scope
        self.done = False
        self._expand_line = self._compile_expand_line(scope)
        self.line = 0

        # We keep track of active conditionals with a stack;
        # elements of the stack are tuples of the form
        # 
        #  (doing_output, have_seen_true, have_seen_else)
        #
        #  doing_output - if we are in a branch where we should output
        #  have_seen_true - if we've already seen a true @@if or @@elif
        #  have_seen_else - if we've already seen @@else
        #
        self.cond_stack = []

    def next(self):
        """Get the next line as per the standard iteration
        protocol. Raises StopIteration if there are no more
        lines"""
        if self.done:
            raise StopIteration

        while True: # Iterate until we return a line to the caller
            self.line = self.line + 1
            try:
                line = self.source.next()
            except StopIteration:
                self.done = True

                if (self.cond_stack):
                    # Change self.line back to the line number of the
                    # start of the @@if
                    self.line = self.cond_stack[-1][3]
                    self._die("@@if is not closed by @@endif")
            
                raise StopIteration

            if (line[0:2] == "@@"):
                self._handle_condition(line)
                continue

            if self._doing_output():
                return self._expand_line(line)

    def __iter__(self):
        """Return the object itself, as per the standard iteration
        protocol"""
        return self

    def _compile_expand_line(self, scope):
        """Return a function that does parameter expansion on
        a line. We do things this way so that we can compile
        the regular expression only once per file and still
        encapsulate the substitution"""
        
        subst = re.compile("@@((?:[a-zA-Z_][a-zA-Z_0-9]*\\.)?[a-zA-Z_][a-zA-Z0-9_]*)@@")
        def repl(m):
            return scope.expand_parameter(m.group(1))
        
        def expand_line(line):
            return subst.sub(repl, line)

        return expand_line

    def _die(self, message):
        """ Print file/line number information with a message and exit """
        print >>sys.stderr, "%s:%d: %s" % (self.display_path, self.line, message)
        sys.exit(1)

    def _doing_output(self):
        """Return True if we are currently writing output"""
        return not self.cond_stack or self.cond_stack[0][0]

    def _true_condition(self, str):
        """Check to see if the argument of @@if or @@elif is true.
        Exits with an error message if the argument isn't valid"""
        str = str.strip();

        # Support literal 'yes' and 'no' to let people
        # conditionalize stuff in and out in #if 0 style
        if str == "yes":
            return True
        elif str == "no":
            return False
        try:
            return self.scope.is_true_parameter(str)
        except KeyError:
            self._die("Parameter name '%s' not recognized" % str)
        except ValueError, e:
            self._die(e)

    def _handle_condition(self, line):
        """Handle a line that is a super directive like @@if.
        Exits with an error message if the line is unrecognized"""
        m = re.match("@@\s*if\s+(.*)", line)
        if m:
            is_true = self._true_condition(m.group(1))
            self.cond_stack.append((is_true, is_true, False, self.line))
            return

        m = re.match("@@\s*elif\s+(.*)", line)
        if m:
            if not self.cond_stack:
                self._die("@@elif without @@if")

            old = self.cond_stack[-1]
            if (old[2]): # Already seen @else
                self._die("@@elif after else")
            if (old[1]): # Already seen something true
                self.cond_stack[-1] = (False, True, False, old[3])
                return

            is_true = self._true_condition(m.group(1))
            self.cond_stack[-1] = (is_true, is_true, False, old[3])
            return

        m = re.match("@@\s*else\s+(.*)", line)
        if m:
            if not self.cond_stack:
                self._die("@@else without @@if")

            old = self.cond_stack[-1]
            if (old[2]): # Already seen @else
                self._die("@@else after @@else")
            if (old[1]): # Already seen something true
                self.cond_stack[-1] = (False, True, True, old[3])
                return

            self.cond_stack[-1] = (True, True, True, old[3])
            return

        m = re.match("@@\s*endif\s+(.*)", line)
        if m:
            if not self.cond_stack:
                self._die("@@endif without @@if")

            self.cond_stack.pop()
            return

        m = re.match("@@\s*error\s+(.*)", line)
        if m:
            if self._doing_output():
                # Support optional quoting
                str = m.group(1).strip()
                str = re.sub('^"(.*)"$','\\1', str)
                self._die(str)
            return

        m = re.match("(@@\s*\S+).*", line)
        self._die("Unrecognized super command: %s" % m.group(1))
