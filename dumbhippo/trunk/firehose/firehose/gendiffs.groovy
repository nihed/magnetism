#!/usr/bin/env groovy

/* utility function */
def zip(List a, List b) {
	aIt = a.iterator()
	bIt = b.iterator()
	List ret = []
	while (aIt.hasNext()) {
		aV = aIt.next()
		bV = bIt.next()
		ret.add([aV, bV])
	}
	return ret
}

def cacheDir = new File(args[0])
def cacheFiles = cacheDir.list({ d,n -> !n.endsWith('.tmp') && !n.endsWith('.diff')} as FilenameFilter)

def urlToSnapshots = new HashMap()
cacheFiles.each { String urlTimestamp ->
	int periodIdx = urlTimestamp.lastIndexOf('.')
	String url = urlTimestamp.substring(0, periodIdx)
	String ts = urlTimestamp.substring(periodIdx+1)
	if (urlToSnapshots[url] == null)
		urlToSnapshots[url] = []
	urlToSnapshots[url].add(ts)
}

urlToSnapshots.each { String url,List timestamps ->
	zip(timestamps.subList(0, timestamps.size()-1), timestamps.subList(1, timestamps.size())).each { ts1, ts2 ->
		ts1Path = new File(cacheDir, "${url}.${ts1}")
		ts2Path = new File(cacheDir, "${url}.${ts2}")
		diffPath = new File(cacheDir, "${url}.${ts1}-${ts2}.diff")
		if (diffPath.exists())
			return
		println "Generating diff between ${ts1Path.getName()} <=> ${ts2Path.getName()}"
		def outStream = diffPath.newOutputStream()
		def pb = new ProcessBuilder("diff", "-u", ts1Path.toString(), ts2Path.toString())
		pb.redirectErrorStream(true)
		def proc = pb.start()
		
		proc.waitFor()
		outStream.close()
	}
}
