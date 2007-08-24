#!/usr/bin/python 

import os,pwd

# A quick hack to enable Bonjour in Pidgin by default

pwfullname = pwd.getpwuid(os.getuid())[4]
pwloginname = pwd.getpwuid(os.getuid())[0]
purple_local_bonjour = '''
<?xml version='1.0' encoding='UTF-8' ?>
<account version='1.0'>
	<account>
		<protocol>prpl-bonjour</protocol>
		<name>%s@localhost</name>
		<alias>%s</alias>
		<settings ui='gtk-gaim'>
			<setting name='auto-login' type='bool'>1</setting>
		</settings>
	</account>
</account>
''' % (pwloginname, pwloginname)
purple_accounts = os.path.expanduser('~/.purple/accounts.xml')
if not os.access(purple_accounts, os.R_OK):
  try:
    os.mkdir(os.path.expanduser('~/.purple'))
  except OSError, e: 
    pass
  f=open(purple_accounts, 'w')
  f.write(purple_local_bonjour)
  f.close()

