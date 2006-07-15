#include "hippo-entity-protected.h"
#include "hippo-person.h"
#include "hippo-chat-room.h"
#include <string.h>

/* === CONSTANTS === */

/* 2 hours entity ignore timeout, in seconds; make this a parameter that can be set */
/* if want different ignore timeouts or want ignore to remain in effect indefinitely */
static const int ENTITY_IGNORE_TIMEOUT = 2*60*60; 

/* === HippoEntity implementation === */

static void     hippo_entity_finalize             (GObject *object);

G_DEFINE_TYPE(HippoEntity, hippo_entity, G_TYPE_OBJECT);

enum {
    CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_entity_init(HippoEntity *entity)
{
}

static void
hippo_entity_class_init(HippoEntityClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    /* This should really just be notify:: on properties probably,
     * but too painful
     */
    signals[CHANGED] =
        g_signal_new ("changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0);

    object_class->finalize = hippo_entity_finalize;
}

static void
hippo_entity_finalize(GObject *object)
{
    HippoEntity *entity = HIPPO_ENTITY(object);

    g_free(entity->guid);
    g_free(entity->name);
    g_free(entity->small_photo_url);

    G_OBJECT_CLASS(hippo_entity_parent_class)->finalize(object); 
}

/* === HippoEntity "protected" API === */

void
hippo_entity_emit_changed(HippoEntity *entity)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    
    g_signal_emit(entity, signals[CHANGED], 0);
}

void
hippo_entity_set_string(HippoEntity *entity,
                        char       **s_p,
                        const char  *val)
{
    if (*s_p == val) /* catches both null, and self assignment */
        return;
    if (*s_p && val && strcmp(*s_p, val) == 0)
        return;        
        
    g_free(*s_p);
    *s_p = g_strdup(val);
    hippo_entity_emit_changed(entity);
}

/* === HippoEntity exported API === */

HippoEntity*
hippo_entity_new(HippoEntityType  type,
                 const char      *guid)
{
    HippoEntity *entity;
    
    if (type == HIPPO_ENTITY_PERSON)
        entity = g_object_new(HIPPO_TYPE_PERSON, NULL);
    else
        entity = g_object_new(HIPPO_TYPE_ENTITY, NULL);
    
    entity->type = type;
    entity->guid = g_strdup(guid);
	entity->date_last_ignored = 0;
    
    return entity;
}

const char*
hippo_entity_get_guid(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->guid;
}

HippoEntityType
hippo_entity_get_entity_type(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), -1);
    return entity->type;
}

const char*
hippo_entity_get_name(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->name;
}

const char*
hippo_entity_get_home_url(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->home_url;
}

const char*
hippo_entity_get_small_photo_url(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->small_photo_url;
}

HippoChatRoom*   
hippo_entity_get_chat_room(HippoEntity    *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), NULL);
    return entity->room;
}

int
hippo_entity_get_chatting_user_count(HippoEntity *entity)
{
	if (entity->room)
		return hippo_chat_room_get_chatting_user_count(entity->room);
	else
		return 0;
}

GTime            
hippo_entity_get_date_last_ignored(HippoEntity   *entity)
{
    g_return_val_if_fail(HIPPO_IS_ENTITY(entity), 0);
    return entity->date_last_ignored;
}

gboolean         
hippo_entity_get_ignored(HippoEntity  *entity)
{
    GTimeVal timeval;

	g_return_val_if_fail(HIPPO_IS_ENTITY(entity), FALSE);

	// date_last_ignored being 0 means that the entity was never ignored 
	// or that the entity was explicitly unignored; we want to check for 
	// it explicitly here, rather than let this special meaning of 0 be lost
	// in the check below
	if (entity->date_last_ignored == 0)
        return FALSE;

    g_get_current_time(&timeval);
	if (entity->date_last_ignored + ENTITY_IGNORE_TIMEOUT < timeval.tv_sec)
		return FALSE;

	return TRUE;
}

void
hippo_entity_set_name(HippoEntity    *entity,
                      const char     *name)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    hippo_entity_set_string(entity, &entity->name, name);
}

void
hippo_entity_set_home_url(HippoEntity    *entity,
                          const char     *url)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    hippo_entity_set_string(entity, &entity->home_url, url);
}

void
hippo_entity_set_small_photo_url(HippoEntity    *entity,
                                 const char     *url)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));
    /* g_debug("Setting photo for '%s' to '%s'", entity->guid, url ? url : "null"); */
    hippo_entity_set_string(entity, &entity->small_photo_url, url);
}

void
hippo_entity_set_chat_room(HippoEntity    *entity,
						   HippoChatRoom  *room)
{
    g_return_if_fail(HIPPO_IS_ENTITY(entity));

    if (room == entity->room)
        return;
            
    if (room)
        g_object_ref(room);
    if (entity->room)
        g_object_unref(entity->room);
    entity->room = room;

    if (entity->room)
        hippo_chat_room_set_title(entity->room, entity->name);
    
    hippo_entity_emit_changed(entity);
}

void             
hippo_entity_set_date_last_ignored(HippoEntity   *entity,
								   GTime          date) 
{
	g_return_if_fail(HIPPO_IS_ENTITY(entity));
    if (entity->date_last_ignored != date) {
        entity->date_last_ignored = date;
        hippo_entity_emit_changed(entity);
    }
}

void             
hippo_entity_set_ignored(HippoEntity    *entity,
						 gboolean        is_ignored)
{
    GTimeVal timeval;

	g_return_if_fail(HIPPO_IS_ENTITY(entity));
    
	if (is_ignored) {
        g_get_current_time(&timeval);
        entity->date_last_ignored = timeval.tv_sec; 
	} else {
        entity->date_last_ignored = 0;
	}
}