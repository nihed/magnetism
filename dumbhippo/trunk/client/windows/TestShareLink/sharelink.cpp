#include <WinSock2.h>
#include <loudmouth/loudmouth.h>

static bool
initializeWinSock(void)
{
    WSADATA wsData;

    // We can support WinSock 2.2
    int result = WSAStartup(MAKEWORD(2,2), &wsData);
    // Fail to initialize if the system doesn't at least of WinSock 2.0
    // Both of these versions are pretty much arbitrary. No testing across
    // a range of versions has been done.
    if (result || LOBYTE(wsData.wVersion) < 2) {
	if (!result)
	    WSACleanup();
	g_printerr("Couldn't initialize WinSock");
	return false;
    }

    return true;
}

int 
main (int argc, char **argv)
{
    static char *link;
    static char *password;
    static char *title;
    static char *username;
    static char **recipients;

    static const GOptionEntry entries[] = {
	{ "link",      'l', 0, G_OPTION_ARG_STRING, (gpointer)&link,     "URL of link to share", "URL" },
	{ "password",  'p', 0, G_OPTION_ARG_STRING, (gpointer)&password, "Login password", "USERNAME" },
	{ "title",     't', 0, G_OPTION_ARG_STRING, (gpointer)&title,    "Title of URL", "TITLE" },
	{ "username",  'u', 0, G_OPTION_ARG_STRING, (gpointer)&username, "Login username", "PASSWORD" },
	{ G_OPTION_REMAINING, 0, 0, G_OPTION_ARG_STRING_ARRAY, (gpointer)&recipients, NULL, NULL },
	{ NULL }
    };

    GOptionContext *context = g_option_context_new("Share a link via dumbhippo.com");
    g_option_context_add_main_entries(context, entries, NULL);

    GError *error = NULL;
    g_option_context_parse(context, &argc, &argv, &error);
    if (error) {
	g_printerr("%s\n", error);
	return 1;
    }

    if (!username || !password || !link || !recipients || !recipients[0]) {
	g_printerr("Usage: sharelink -u USER -p PASS -l URL [-t TITLE] recipient [recipient...]");
	return 1;
    }

    if (!initializeWinSock()) {
	return 1;  
    }

    LmConnection *connection = lm_connection_new("messages.dumbhippo.com");
    if (!lm_connection_open_and_block(connection, &error)) {
	g_printerr("Couldn't connect to server: %s\n", error->message);
	return 1;
    }
    if (!lm_connection_authenticate_and_block(connection, username, password, "sharelink", &error)) {
	g_printerr("Couldn't authenticate to server: %s\n", error->message);
	return 1;
    }

    for (char **recipient = recipients; *recipient; recipient++) {
	char *to = g_strdup_printf("%s@dumbhippo.com", *recipient);

	LmMessage *message = lm_message_new_with_sub_type(to, 
							  LM_MESSAGE_TYPE_MESSAGE,
							  LM_MESSAGE_SUB_TYPE_HEADLINE);

	lm_message_node_add_child(message->node, "subject", "New link");

	gchar *bodyString;
	if (title)
	    bodyString = g_strdup_printf("Check out this link: %s (%s)", title, link);
	else
	    bodyString = g_strdup_printf("Check out this link: %s", link);

	lm_message_node_add_child(message->node, "body", bodyString);
	g_free(bodyString);

	gchar *htmlString;
	if (title)
	    htmlString = g_strdup_printf("<body xmlns='http://www.w3.org/1999/xmhtml'><p>Check out this link: <a href='%s'>%s</a></p></body>", link, title);
	else
	    htmlString = g_strdup_printf("<body xmlns='http://www.w3.org/1999/xmhtml'><p>Check out this link: <a href='%s'>%s</a></p></body>", link, link);

	LmMessageNode *htmlNode = lm_message_node_add_child(message->node, "html", htmlString);
	lm_message_node_set_attribute(htmlNode, "xmlns", "http://jabber.org/protocol/xhtml-im");
	lm_message_node_set_raw_mode(htmlNode, TRUE);
	g_free(htmlString);

	LmMessageNode *linkNode = lm_message_node_add_child(message->node, "link", NULL);
	lm_message_node_set_attribute(linkNode, "xmlns", "http://dumbhippo.com/protocol/linkshare");
	lm_message_node_set_attribute(linkNode, "href", link);
	if (title)
	    lm_message_node_add_child(linkNode, "title", title);

	if (!lm_connection_send(connection, message, &error)) {
	    g_printerr("Error sending to %s: %s\n", to, error->message);
	}
	
	g_free(to);
	lm_message_unref(message);
    }

    WSACleanup();

    return 0;
}
