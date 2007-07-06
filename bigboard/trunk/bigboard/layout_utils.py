import copy

def compute_lengths(allocated, min_lengths, natural_lengths, expand_map=None):
    count = len(min_lengths)

    total_natural = sum(natural_lengths)

    to_shrink = total_natural - allocated

    if to_shrink > 0:
        lengths = copy.copy(natural_lengths)
        # We were allocated less than our natural height. We want to shrink lines
        # as equally as possible, but no line more than it's maximum shrink.
        #
        # To do this, we process the lines in order of the available shrink from
        # least available shrink to most
        #
        shrinks = []
        for i in xrange(0, count):
            shrinks.append((i, natural_lengths[i] - min_lengths[i]))
            shrinks.sort(key=lambda t: t[1])
            
        lines_remaining = count
        for (i, shrink) in shrinks:
            # If we can shrink the rest of the lines equally, do that. Otherwise
            # shrink this line as much as possible
            if shrink * lines_remaining >= to_shrink:
                shrink = to_shrink // lines_remaining
                
            lengths[i] -= shrink
            lines_remaining -= 1
            to_shrink -= shrink
            
        return lengths
    elif to_shrink < 0 and expand_map != None and len(expand_map) > 0:
        expand_count = len(expand_map)
        lengths = copy.copy(natural_lengths)
        to_grow = - to_shrink
            
        for i in xrange(0, count):
            if i in expand_map:
                delta = to_grow // expand_count
                lengths[i] += delta
                to_grow -= delta
                expand_count -= 1

        return lengths
    else:
        return natural_lengths
