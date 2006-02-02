from super.service import Service
import re

trimre = re.compile('[0-9]+-[0-9]+-[0-9]+\s+')

reloadre = re.compile('Deployed package:.*dumbhippo.ear')
ignorere = re.compile('\[STDOUT\]')
infore = re.compile('INFO')
warnre = re.compile('WARN')
errorre = re.compile('ERROR')
hippore = re.compile('DEBUG \[com.dumbhippo')
hippoclassre = re.compile('com.dumbhippo.([a-zA-Z.]+)')

class JBossService(Service):

    """ Specialization of Service for JBoss """
    
    def watchLine(self, line):
        if ignorere.search(line):
            return
        if reloadre.search(line):
            self.playSound(self.expand_parameter("reloadSound"))

        # Trim off the date
        line = trimre.sub('', line)

        if infore.search(line):
            print "\033[34m" + line + "\033[m" # blue
        elif warnre.search(line):
            print "\033[31m" + line + "\033[m" # red
        elif errorre.search(line):
            print "\033[31m" + line + "\033[m" # red
        elif hippore.search(line):
            # Compactify com.dumbhippo. classes, mark in green
            print hippoclassre.sub("\033[32m\\1\033[m", line)
