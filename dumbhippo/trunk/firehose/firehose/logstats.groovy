#!/usr/bin/env groovy

import java.text.SimpleDateFormat

def updatePattern = ~/splitting ([0-9]+) tasks into messages/

def asctimeFormat = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss,SSS')

def logf = new File(args[0])
def firstDate = null
def lastDate = null
def total = 0
logf.eachLine { line ->
    def elts = line.split(' ', 5)
    def msg = elts[4]
	def matcher = updatePattern.matcher(msg)
	if (!matcher.matches())
		return
	def count = Integer.parseInt(matcher.group(1))
	total += count
	
    def date = asctimeFormat.parse(elts[0] + ' ' + elts[1])
	if (firstDate == null)
		firstDate = date
	lastDate = date
}
def overSeconds = (lastDate.getTime() - firstDate.getTime()) / 1000
println "total: ${total} in ${overSeconds} seconds"
