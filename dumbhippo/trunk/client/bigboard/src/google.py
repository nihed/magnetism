import httplib2, keyring, libbig, sys

class Google:

    def __init__(self):
        k = keyring.get_keyring()

        username, password = k.get_login("google")

        self.__username = username
        self.__password = password

    def get_calendar(self):
        h = httplib2.Http()
        h.add_credentials(self.__username, self.__password)
        h.follow_all_redirects = True
        uri = 'http://www.google.com/calendar/feeds/' + self.__username + '@gmail.com/private/full'
        
        return h.request(uri, "GET", headers = {})
        
if __name__ == '__main__':

    libbig.set_application_name("BigBoard")

    keyring.get_keyring().store_login('google', 'havoc.pennington', '')

    g = Google()

    events = g.get_calendar()

    print events

    
    
