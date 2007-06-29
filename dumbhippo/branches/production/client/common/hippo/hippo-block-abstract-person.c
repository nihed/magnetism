/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_abstract_person_init                (HippoBlockAbstractPerson       *block_abstract_person);
static void      hippo_block_abstract_person_class_init          (HippoBlockAbstractPersonClass  *klass);

static void      hippo_block_abstract_person_dispose             (GObject              *object);
static void      hippo_block_abstract_person_finalize            (GObject              *object);

static void hippo_block_abstract_person_set_property (GObject      *object,
                                                      guint         prop_id,
                                                      const GValue *value,
                                                      GParamSpec   *pspec);
static void hippo_block_abstract_person_get_property (GObject      *object,
                                                      guint         prop_id,
                                                      GValue       *value,
                                                      GParamSpec   *pspec);

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_USER
};

G_DEFINE_TYPE(HippoBlockAbstractPerson, hippo_block_abstract_person, HIPPO_TYPE_BLOCK);

static void
hippo_block_abstract_person_init(HippoBlockAbstractPerson *block_abstract_person)
{
}

static void
hippo_block_abstract_person_class_init(HippoBlockAbstractPersonClass *klass)
{
    /* HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass); */
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_abstract_person_set_property;
    object_class->get_property = hippo_block_abstract_person_get_property;

    object_class->dispose = hippo_block_abstract_person_dispose;
    object_class->finalize = hippo_block_abstract_person_finalize;

    g_object_class_install_property(object_class,
                                    PROP_USER,
                                    g_param_spec_object("user",
                                                        _("User"),
                                                        _("User the per-person block is for"),
                                                        HIPPO_TYPE_PERSON,
                                                        G_PARAM_READABLE));
}

static void
set_user(HippoBlockAbstractPerson *block_abstract_person,
         HippoPerson              *user)
{
    if (user == block_abstract_person->user)
        return;

    if (block_abstract_person->user) {
        g_object_unref(block_abstract_person->user);
        block_abstract_person->user = NULL;
    }

    if (user) {
        g_object_ref(user);
        block_abstract_person->user = user;
    }

    g_object_notify(G_OBJECT(block_abstract_person), "user");
}

static void
hippo_block_abstract_person_dispose(GObject *object)
{
    HippoBlockAbstractPerson *block_abstract_person = HIPPO_BLOCK_ABSTRACT_PERSON(object);

    set_user(block_abstract_person, NULL);

    G_OBJECT_CLASS(hippo_block_abstract_person_parent_class)->dispose(object);
}

static void
hippo_block_abstract_person_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_abstract_person_parent_class)->finalize(object);
}

static void
hippo_block_abstract_person_set_property(GObject         *object,
                                         guint            prop_id,
                                         const GValue    *value,
                                         GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_block_abstract_person_get_property(GObject         *object,
                                         guint            prop_id,
                                         GValue          *value,
                                         GParamSpec      *pspec)
{
    HippoBlockAbstractPerson *block_abstract_person = HIPPO_BLOCK_ABSTRACT_PERSON(object);

    switch (prop_id) {
    case PROP_USER:
        g_value_set_object(value, G_OBJECT(block_abstract_person->user));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

void
hippo_block_abstract_person_set_user(HippoBlockAbstractPerson *block_person,
                                     HippoPerson              *user)
{
    set_user(block_person, user);
}

HippoPerson*
hippo_block_abstract_person_get_user(HippoBlockAbstractPerson *block_person)
{
    return block_person->user;
}
