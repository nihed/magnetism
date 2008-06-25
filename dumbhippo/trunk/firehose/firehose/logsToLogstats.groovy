# This script processes log files named "server.log.$date" into 
# files named logstats.$date
# Usage: groovy -cp /usr/share/java/commons-io.jar /home/cluster/dhdeploy/firehose/logs
# Intended to be run daily-ish, preferably after logs are rotated

import org.apache.commons.io.IOUtils

println "Scanning ${args[0]}..."
def logdir = new File(args[0])

def logPrefix = "server.log."
def logfiles = logdir.list({ d,f -> f.startsWith(logPrefix) } as FilenameFilter)
logfiles.each { name ->
  def path = new File(logdir, name)
  def outName = "logstats." + name.substring(logPrefix.length())
  def outPath = new File(logdir, outName)
  
  if (outPath.exists())
    return;  
 
  def outtmp = File.createTempFile("logstats", ".tmp", logdir)
  def outTmpStream = new BufferedOutputStream(new FileOutputStream(outtmp))
  def procBuilder = new ProcessBuilder(new File(".", "logstats.groovy").toString(), path.toString());
  def proc = procBuilder.start()
  IOUtils.copy(proc.getInputStream(), outTmpStream)
  proc.waitFor()
  outTmpStream.close()

  println "Renaming to ${outPath}"
  outtmp.renameTo(outPath)
}
println "done."