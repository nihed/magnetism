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
                <statuses>
                        <status type='available' name='Available' active='true'>
                                <attributes/>
                        </status>
                        <status type='away' name='Away' active='false'>
                                <attributes/>
                        </status>
                        <status type='offline' name='Offline' active='false'>
                                <attributes/>
                        </status>
                </statuses>
                <settings>
                        <setting name='first' type='string'>guest</setting>
                        <setting name='last' type='string'></setting>
                        <setting name='AIM' type='string'></setting>
                        <setting name='jid' type='string'></setting>
                        <setting name='email' type='string'></setting>
                </settings>
                <settings ui='gtk-gaim'>
                        <setting name='auto-login' type='bool'>1</setting>
                </settings>
                <proxy>
                        <type>none</type>
                </proxy>
        </account>
</account>
''' % (pwloginname,)
purple_blist_default = '''
<?xml version='1.0' encoding='UTF-8' ?>

<purple version='1.0'>
        <blist/>
        <privacy>
                <account proto='prpl-bonjour' name='%s@localhost' mode='1'/>
        </privacy>
</purple>
''' % (pwloginname,)
purple_accounts = os.path.expanduser('~/.purple/accounts.xml')
if not os.access(purple_accounts, os.R_OK):
  try:
    os.mkdir(os.path.expanduser('~/.purple'))
  except OSError, e: 
    pass
  f=open(purple_accounts, 'w')
  f.write(purple_local_bonjour)
  f.close()
purple_blist = os.path.expanduser("~/.purple/blist.xml")
if not os.access(purple_blist, os.R_OK):
  f=open(purple_blist, 'w')
  f.write(purple_blist_default)
  f.close()

