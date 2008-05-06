#!/usr/bin/env groovy

import java.text.SimpleDateFormat

long timeSliceMilliseconds = 1*60*60*1000 /* 1 hour */

def updatePattern = ~/splitting ([0-9]+) tasks into messages/
def fullUnmodifiedFetchPattern = ~/Fetched full unmodified content for (.+)$/
def unmodifiedFetchPattern = ~/Got 304 Unmodified for (.+)$/
def modifiedFetchPattern = ~/Got new hash.+for url (.+)$/
def asctimeFormat = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss,SSS')

/* This works for ASCII at least */
def parsePyUnicodeRepr(String s) {
	return s.substring(2, s.length()-1)
}

println "Parsing ${args[0]}..."
def logf = new File(args[0])
long tsStart = 0
long nextCutoff = 0
long tsEnd = 0
int total = 0
def resultGroups = []
class ResultGroup {
	long startDate
	long updates = 0
	def domainUnmodified = new HashMap()
	def domainModified = new HashMap()
}
def currentResultGroup = null
/* Check whether current line matches the pattern; if so parse out
 * the domain and increment its count in given hash
 */
def incrementHashIfMatches(String msg, pattern, hash) {
	def matcher = pattern.matcher(msg)
	if (matcher.matches()) {
    	def domain = new URL(parsePyUnicodeRepr(matcher.group(1))).host
    	if (!hash.containsKey(domain)) {
    		hash[domain] = 1
    	} else {
    		hash[domain] += 1
    	}	
    	return true;
	}
	return false;
}
logf.eachLine { line ->
	if (!line.startsWith('2'))
		return
    def elts = line.split(' ', 5)
    String msg = elts[4]
    /* First check to see if this is a total count change */
	def matcher = updatePattern.matcher(msg)
	if (matcher.matches()) {
		int updateCount = Integer.parseInt(matcher.group(1))
		total += updateCount
		if (currentResultGroup != null)
			currentResultGroup.updates += updateCount
	    long date = asctimeFormat.parse(elts[0] + ' ' + elts[1]).getTime()
		if (tsStart == 0 || date - tsStart > timeSliceMilliseconds) {
			if (tsStart != 0) {
				resultGroups.add(currentResultGroup)
			}
			currentResultGroup = new ResultGroup(startDate: date)
			tsStart = date
		}
	    tsEnd = date
	    return
	} 
    /* If we don't have a current group, wait until we do */
    if (currentResultGroup == null)
    	return
    if (incrementHashIfMatches(msg, fullUnmodifiedFetchPattern, currentResultGroup.domainUnmodified))
    	return
    if (incrementHashIfMatches(msg, unmodifiedFetchPattern, currentResultGroup.domainUnmodified))
    	return    	
    if (incrementHashIfMatches(msg, modifiedFetchPattern, currentResultGroup.domainModified))
    	return
}

def printTopKeys(String prefix, domainHash, other) {
	def keys = new ArrayList(domainHash.keySet())
	keys.sort({ a,b -> domainHash[b].compareTo(domainHash[a])})
	keys.subList(0, 5).each { k ->
		def otherValue = other != null ? other[k] : null;
		print " ${prefix} ${k} -> ${domainHash[k]}"
		if (otherValue != null)
			println " (${otherValue})"
		else
			println ""	
	}
}

print "Got ${resultGroups.size()} groups"
resultGroups.each { group ->
    Date d = new Date(group.startDate)
    def pollsPerSec = group.updates/(timeSliceMilliseconds/1000)
	println "updates from ${d}: ${group.updates} (${pollsPerSec} checks per second)"
	printTopKeys("U", group.domainUnmodified, group.domainModified)
	printTopKeys("M", group.domainModified, group.domainUnmodified)
}
