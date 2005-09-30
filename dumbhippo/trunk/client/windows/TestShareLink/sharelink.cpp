#include <WinSock2.h>
#include <loudmouth/loudmouth.h>
#include <stdio.h>

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

static char *
readDescription(void)
{
    GString *descriptionRaw = g_string_new(NULL);
    g_print("Enter link description, finishing with a line with a single . character\n");
    while (TRUE) {
	char buf[1024];
	if (!fgets(buf, G_N_ELEMENTS(buf), stdin))
	    break;
	if (strcmp(buf, ".") == 0 || strcmp(buf, ".\n") == 0)
	    break;
	g_string_append (descriptionRaw, buf);
    }
    char *description = g_locale_to_utf8(descriptionRaw->str, descriptionRaw->len, NULL, NULL, NULL);
    g_string_free (descriptionRaw, TRUE);

    if (description) {
	g_strstrip(description);
	if (strcmp (description, "") == 0) {
	    g_free (description);
	    description = NULL;
	}
    }

    return description;
}

LmMessage *
createMessage(char *to, char *link, char *title, char *description)
{
    LmMessage *message = lm_message_new_with_sub_type(to, 
 	                                              LM_MESSAGE_TYPE_MESSAGE,
	                                              LM_MESSAGE_SUB_TYPE_HEADLINE);

    lm_message_node_add_child(message->node, "subject", "New link");



    GString *bodyString = g_string_new("Check out this link: ");

    if (title)
	g_string_append_printf(bodyString, "%s (%s). ", title, link);
    else
	g_string_append_printf(bodyString, "%s. ", link);

    if (description)
	g_string_append (bodyString, description);

    lm_message_node_add_child(message->node, "body", bodyString->str);
    g_string_free(bodyString, TRUE);

    char *htmlLink;
    htmlLink = g_markup_printf_escaped("<a href='%s'>%s</a>", link, title ? title : link);

    char *htmlDescription;
    if (description)
	htmlDescription = g_markup_printf_escaped("<br/><p>%s</p>", description);
    else
	htmlDescription = g_strdup("");

    char *htmlString;
    htmlString = g_strdup_printf("<body xmlns='http://www.w3.org/1999/xmhtml'><p>Check out this link: %s</p>%s</body>", 
	htmlLink, htmlDescription);

    LmMessageNode *htmlNode = lm_message_node_add_child(message->node, "html", htmlString);
    lm_message_node_set_attribute(htmlNode, "xmlns", "http://jabber.org/protocol/xhtml-im");
    lm_message_node_set_raw_mode(htmlNode, TRUE);
    g_free(htmlString);
    g_free(htmlLink);
    g_free(htmlDescription);

    LmMessageNode *linkNode = lm_message_node_add_child(message->node, "link", NULL);
    lm_message_node_set_attribute(linkNode, "xmlns", "http://dumbhippo.com/protocol/linkshare");
    lm_message_node_set_attribute(linkNode, "href", link);
    if (title)
	lm_message_node_add_child(linkNode, "title", title);
    if (description)
	lm_message_node_add_child(linkNode, "description", description);

    return message;
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

    //LmConnection *connection = lm_connection_new("messages.dumbhippo.com");
    LmConnection *connection = lm_connection_new("192.168.1.10");
    if (!lm_connection_open_and_block(connection, &error)) {
	g_printerr("Couldn't connect to server: %s\n", error->message);
	return 1;
    }
    if (!lm_connection_authenticate_and_block(connection, username, password, "sharelink", &error)) {
	g_printerr("Couldn't authenticate to server: %s\n", error->message);
	return 1;
    }

    char *description = readDescription();

    for (char **recipient = recipients; *recipient; recipient++) {
	char *to = g_strdup_printf("%s@dumbhippo.com", *recipient);

	LmMessage *message = createMessage(to, link, title, description);
	
	if (!lm_connection_send(connection, message, &error))
	    g_printerr("Error sending to %s: %s\n", to, error->message);
	
	g_free(to);
	lm_message_unref(message);
    }

    WSACleanup();

    return 0;
}
