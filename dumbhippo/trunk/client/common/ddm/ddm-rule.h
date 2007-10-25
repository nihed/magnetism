/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __DDM_RULE_H__
#define __DDM_RULE_H__

#include "ddm-data-resource.h"

G_BEGIN_DECLS

typedef struct _DDMCondition DDMCondition;
typedef struct _DDMConditionValue DDMConditionValue;

typedef struct _DDMRule DDMRule;

typedef enum {
    DDM_CONDITION_AND,
    DDM_CONDITION_OR,
    DDM_CONDITION_NOT,
    DDM_CONDITION_EQUAL
} DDMConditionType;

typedef enum {
    DDM_CONDITION_VALUE_SOURCE_PROPERTY,
    DDM_CONDITION_VALUE_TARGET_PROPERTY,
    DDM_CONDITION_VALUE_BOOLEAN,
    DDM_CONDITION_VALUE_INTEGER,
    DDM_CONDITION_VALUE_FLOAT,
    DDM_CONDITION_VALUE_STRING,
} DDMConditionValueType;

struct _DDMConditionValue {
    DDMConditionValueType type;
    
    union {
        gboolean boolean;
        gint64 integer;
        double float_;
        char *string;
    } u;
};

typedef struct {
    DDMCondition *left;
    DDMCondition *right;
} DDMConditionBinary;

typedef struct {
    DDMCondition *child;
} DDMConditionUnary;

typedef struct {
    DDMConditionValue left;
    DDMConditionValue right;
} DDMConditionEqual;

struct _DDMCondition {
    DDMConditionType type;

    union {
        DDMConditionBinary and;
        DDMConditionBinary or;
        DDMConditionUnary not;
        DDMConditionEqual equal;
    } u;
};

DDMCondition *ddm_condition_from_string (const char   *str);
char         *ddm_condition_to_string   (DDMCondition *condition);

DDMCondition *ddm_condition_new_equal (DDMConditionValue *left,
                                       DDMConditionValue *right);
DDMCondition *ddm_condition_new_not   (DDMCondition      *child);
DDMCondition *ddm_condition_new_and   (DDMCondition      *left,
                                       DDMCondition      *right);
DDMCondition *ddm_condition_new_or    (DDMCondition      *left,
                                       DDMCondition      *right);

void          ddm_condition_free        (DDMCondition *condition);

gboolean      ddm_condition_matches_source(DDMDataResource *source_resource);
gboolean      ddm_condition_matches_target(DDMDataResource *target_resource);

DDMRule      *ddm_rule_new (const char *target_class_id,
                            const char *target_property,
                            const char *source_class_id,
                            const char *condition);

DDMCondition *ddm_rule_build_target_condition(DDMDataResource *source_resource);
DDMCondition *ddm_rule_build_source_condition(DDMDataResource *target_resource);

G_END_DECLS

#endif /* __DDM_RULE_H__ */
