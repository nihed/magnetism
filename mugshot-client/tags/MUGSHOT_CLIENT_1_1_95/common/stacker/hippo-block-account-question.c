/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-internal.h"
#include "hippo-block-account-question.h"
#include <string.h>

static void      hippo_block_account_question_init                (HippoBlockAccountQuestion       *block_account_question);
static void      hippo_block_account_question_class_init          (HippoBlockAccountQuestionClass  *klass);

static void      hippo_block_account_question_dispose             (GObject              *object);
static void      hippo_block_account_question_finalize            (GObject              *object);

static void      hippo_block_account_question_update              (HippoBlock           *block);

static void hippo_block_account_question_set_property (GObject      *object,
                                                       guint         prop_id,
                                                       const GValue *value,
                                                       GParamSpec   *pspec);
static void hippo_block_account_question_get_property (GObject      *object,
                                                       guint         prop_id,
                                                       GValue       *value,
                                                       GParamSpec   *pspec);

static HippoAccountQuestionButton *hippo_account_question_button_new  (const char                 *text,
                                                                       const char                 *response);
static void                        hippo_account_question_button_free (HippoAccountQuestionButton *button);

struct _HippoBlockAccountQuestion {
    HippoBlock            parent;

    char *answer;
    char *description;
    char *more_link;
    GSList *buttons;
};

struct _HippoBlockAccountQuestionClass {
    HippoBlockClass parent_class;
};

struct _HippoAccountQuestionButton {
    char *text;
    char *response;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_ANSWER,
    PROP_DESCRIPTION,
    PROP_BUTTONS,
    PROP_MORE_LINK
};

G_DEFINE_TYPE(HippoBlockAccountQuestion, hippo_block_account_question, HIPPO_TYPE_BLOCK);
                       
static void
hippo_block_account_question_init(HippoBlockAccountQuestion *block_account_question)
{
}

static void
hippo_block_account_question_class_init(HippoBlockAccountQuestionClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_account_question_set_property;
    object_class->get_property = hippo_block_account_question_get_property;

    object_class->dispose = hippo_block_account_question_dispose;
    object_class->finalize = hippo_block_account_question_finalize;

    block_class->update = hippo_block_account_question_update;
    
    g_object_class_install_property(object_class,
                                    PROP_ANSWER,
                                    g_param_spec_string("answer",
                                                        _("Answer"),
                                                        _("User's answer to the question (may be null)"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_DESCRIPTION,
                                    g_param_spec_string("description",
                                                        _("Description"),
                                                        _("Detailed description of the question"),
                                                        NULL,
                                                        G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_BUTTONS,
                                    g_param_spec_pointer("buttons",
                                                         _("Buttons"),
                                                         _("GSList of HippoAccountQuestionButton"),
                                                         G_PARAM_READABLE));

    g_object_class_install_property(object_class,
                                    PROP_MORE_LINK,
                                    g_param_spec_string("more-link",
                                                        _("More Link"),
                                                        _("URL for more information about the question"),
                                                        NULL,
                                                        G_PARAM_READABLE));

}

static void
set_answer(HippoBlockAccountQuestion *block_account_question,
           const char                *answer)
{
    if (answer == block_account_question->answer ||
        (answer && block_account_question->answer && strcmp(answer, block_account_question->answer) == 0))
        return;

    g_free(block_account_question->answer);
    block_account_question->answer = g_strdup(answer);

    g_object_notify(G_OBJECT(block_account_question), "answer");
}

static void
set_description(HippoBlockAccountQuestion *block_account_question,
                const char                *description)
{
    if (description == block_account_question->description ||
        (description && block_account_question->description && strcmp(description, block_account_question->description) == 0))
        return;

    g_free(block_account_question->description);
    block_account_question->description = g_strdup(description);

    g_object_notify(G_OBJECT(block_account_question), "description");
}

/* Assumes ownership */
static void
set_buttons(HippoBlockAccountQuestion *block_account_question,
            GSList                    *buttons)
{
    if (block_account_question->buttons) {
        g_slist_foreach(block_account_question->buttons, (GFunc)hippo_account_question_button_free, NULL);
        g_slist_free(block_account_question->buttons);
    }

    block_account_question->buttons = buttons;

    g_object_notify(G_OBJECT(block_account_question), "buttons");
}

static void
set_more_link(HippoBlockAccountQuestion *block_account_question,
                const char              *more_link)
{
    if (more_link == block_account_question->more_link ||
        (more_link && block_account_question->more_link && strcmp(more_link, block_account_question->more_link) == 0))
        return;

    g_free(block_account_question->more_link);
    block_account_question->more_link = g_strdup(more_link);

    g_object_notify(G_OBJECT(block_account_question), "more-link");
}

static void
hippo_block_account_question_dispose(GObject *object)
{
    HippoBlockAccountQuestion *block_account_question = HIPPO_BLOCK_ACCOUNT_QUESTION(object);

    set_answer(block_account_question, NULL);
    set_description(block_account_question, NULL);
    set_buttons(block_account_question, NULL);
    set_more_link(block_account_question, NULL);
    
    G_OBJECT_CLASS(hippo_block_account_question_parent_class)->dispose(object); 
}

static void
hippo_block_account_question_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_account_question_parent_class)->finalize(object); 
}

static void
hippo_block_account_question_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_block_account_question_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoBlockAccountQuestion *block_account_question = HIPPO_BLOCK_ACCOUNT_QUESTION(object);

    switch (prop_id) {
    case PROP_ANSWER:
        g_value_set_string(value, block_account_question->answer);
        break;
    case PROP_DESCRIPTION:
        g_value_set_string(value, block_account_question->description);
        break;
    case PROP_BUTTONS:
        g_value_set_pointer(value, block_account_question->buttons);
        break;
    case PROP_MORE_LINK:
        g_value_set_string(value, block_account_question->more_link);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}


static void
hippo_block_account_question_update (HippoBlock *block)
{
    HippoBlockAccountQuestion *block_account_question = HIPPO_BLOCK_ACCOUNT_QUESTION(block);
    const char *description = NULL;
    const char *more_link = NULL;
    const char *answer = NULL;
    GSList *button_strings = NULL;
    GSList *buttons = NULL;
    GSList *l;

    HIPPO_BLOCK_CLASS(hippo_block_account_question_parent_class)->update(block);

    ddm_data_resource_get(block->resource,
                          "description", DDM_DATA_STRING, &description,
                          "moreLink", DDM_DATA_URL, &more_link,
                          "answer", DDM_DATA_STRING, &answer,
                          "buttons", DDM_DATA_STRING | DDM_DATA_LIST, &button_strings,
                          NULL);

    for (l = button_strings; l; l = l->next) {
        const char *str = l->data;
        const char *colon = strchr(l->data, ':');
        char *response;
        
        if (colon == NULL) {
            g_warning("Button isn't of the form response:Text");
            continue;
        }

        response = g_strndup(str, colon - str);
        buttons = g_slist_prepend(buttons, hippo_account_question_button_new(colon + 1, response));
        g_free(response);
    }
    
    buttons = g_slist_reverse(buttons);
            
    set_answer(block_account_question, answer);
    set_description(block_account_question, description);
    set_more_link(block_account_question, more_link);
    hippo_block_set_pinned(block, answer == NULL);
    set_buttons(block_account_question, buttons); /* Assumes ownership */
}

static HippoAccountQuestionButton *
hippo_account_question_button_new(const char *text,
                                  const char *response)
{
    HippoAccountQuestionButton *button = g_new0(HippoAccountQuestionButton, 1);
    button->text = g_strdup(text);
    button->response = g_strdup(response);

    return button;
}

static void
hippo_account_question_button_free(HippoAccountQuestionButton *button)
{
    g_free(button->text);
    g_free(button->response);

    g_free(button);
}

const char *
hippo_account_question_button_get_text(HippoAccountQuestionButton *button)
{
    return button->text;
}

const char *
hippo_account_question_button_get_response(HippoAccountQuestionButton *button)
{
    return button->response;
}


