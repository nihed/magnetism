/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <glib.h>
#include <glib-object.h>
#include <stdlib.h>
#include "src/hippo-dbus-client.h"
#include "hippo-dbus-ipc-locator.h"
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include <stdarg.h>

static HippoIpcController *controller = NULL;
static HippoIpcListener *listener = NULL;
static HippoEndpointId endpoint = 0;
static const char *chatRoom = NULL;

class TestListener : public HippoIpcListener {
public:    
    virtual void onConnect();
    virtual void onDisconnect();
    virtual void onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant);
    virtual void onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId);
    virtual void onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, int sentiment,double timestamp, long serial);
    virtual void userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying);
    virtual void applicationInfo(HippoEndpointId endpoint, const char *applicationId, bool canInstall, bool canRun, const char *version);
};

void
TestListener::onConnect()
{
    g_print("connect, current endpoint is %lld\n", endpoint);
    if (endpoint == 0) {
        endpoint = controller->registerEndpoint(listener);
        if (endpoint != 0) {
            g_printerr("registered, new endpoint %lld\n", endpoint);
            if (chatRoom)
                controller->joinChatRoom(endpoint, chatRoom, true);
        } else {
            g_printerr("Not able to register yet, endpoint still 0\n");
        }
    }
}

void
TestListener::onDisconnect()
{
    g_print("disconnect, endpoint was %lld, forgetting it\n", endpoint);
    if (endpoint != 0) {
        endpoint = 0;
    }
}

void
TestListener::onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant)
{
    g_print("userJoin\n");
    g_print("    chatId: %s\n", chatId);
    g_print("    userId: %s\n", userId);
    g_print("    participant: %d\n", participant);
}

void
TestListener::onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId)
{
    g_print("userLeave\n");
    g_print("    chatId: %s\n", chatId);
    g_print("    userId: %s\n", userId);
}

void
TestListener::onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, int sentiment, double timestamp, long serial)
{
    g_print("message\n");
    g_print("    chatId: %s\n", chatId);
    g_print("    userId: %s\n", userId);
    g_print("    message: %s\n", message);
    g_print("    sentiment: %d\n", sentiment);
    g_print("    timestamp: %f\n", timestamp);
    g_print("    serial: %ld\n", serial);
}

void
TestListener::userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying)
{
    g_print("userInfo\n");
    g_print("    userId: %s\n", userId);
    g_print("    name: %s\n", name);
    g_print("    smallPhotoUrl: %s\n", smallPhotoUrl);
    g_print("    currentSong: %s\n", currentSong);
    g_print("    currentArtist: %s\n", currentArtist);
    g_print("    musicPlaying: %d\n", musicPlaying);
}

void
TestListener::applicationInfo(HippoEndpointId endpoint, const char *applicationId, bool canInstall, bool canRun, const char *version)
{
    g_print("applicationInfo\n");
    g_print("    userId: %s\n", applicationId);
    g_print("    canInstall: %d\n", canInstall);
    g_print("    canRun: %d\n", canRun);
    g_print("    version: %s\n", version);
}

static const void
fatal(const char *format, ...)
{
    va_list vap;
    char *msg;
    
    va_start(vap, format);
    msg = g_strdup_vprintf(format, vap);
    va_end(vap);
    
    g_printerr("%s\n", msg);
    exit(1);
}

int main(int argc, char **argv)
{
    const char *serverName = NULL;
    
    g_type_init();

    if (argc != 2 && argc != 3) {
	fatal("Usage: test-hippo-ipc SERVER_NAME [CHAT_ROOM_ID]");
    }

    serverName = argv[1];
    if (argc == 3)
	chatRoom = argv[2];
    
    controller = HippoDBusIpcLocator::getInstance()->getController(argv[1]);
    listener = new TestListener();
    
    controller->addListener(listener);
    // assume an initial connection opportunity
    listener->onConnect();
    
    GMainLoop *loop = g_main_loop_new(NULL, FALSE);
    g_main_loop_run(loop);
    
    HippoDBusIpcLocator::getInstance()->releaseController(controller);

    return 0;
}
