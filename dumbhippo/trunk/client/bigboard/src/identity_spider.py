import pwd, os

class IdentitySpider:
    def get_self_name(self):
        return pwd.getpwuid(os.getuid()).pw_gecos