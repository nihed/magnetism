/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __DDM_RULE_H__
#define __DDM_RULE_H__

#include "ddm-data-fetch.h"
#include "ddm-data-resource.h"

G_BEGIN_DECLS

typedef struct _DDMCondition DDMCondition;
typedef struct _DDMConditionValue DDMConditionValue;

typedef struct _DDMRule DDMRule;

typedef enum {
    DDM_CONDITION_AND,
    DDM_CONDITION_OR,
    DDM_CONDITION_NOT,
    DDM_CONDITION_EQUAL,
    DDM_CONDITION_TRUE,
    DDM_CONDITION_FALSE
} DDMConditionType;

typedef enum {
    DDM_CONDITION_VALUE_SOURCE,
    DDM_CONDITION_VALUE_TARGET,
    DDM_CONDITION_VALUE_SOURCE_PROPERTY,
    DDM_CONDITION_VALUE_TARGET_PROPERTY,
    DDM_CONDITION_VALUE_RESOURCE,
    DDM_CONDITION_VALUE_PROPERTY,
    DDM_CONDITION_VALUE_BOOLEAN,
    DDM_CONDITION_VALUE_INTEGER,
    DDM_CONDITION_VALUE_STRING,
} DDMConditionValueType;

struct _DDMConditionValue {
    DDMConditionValueType type;
    
    union {
        gboolean boolean;
        gint64 integer;
        /* Float is intentionally missing here since the only form
         * of comparison we support is exact equality. If you add
         * less-than, greater-than comparions, it might be useful
         * to add floats as well.
         */
        char *string;
        DDMDataResource *resource;
        DDMDataProperty *property;
    } u;
};

struct _DDMRule {
    char *target_class_id;
    DDMQName *target_property;
    char *source_class_id;
    DDMDataCardinality cardinality;
    gboolean default_include;
    DDMDataFetch *default_children;
    DDMCondition *condition;
};

typedef struct {
    DDMCondition *left;
    DDMCondition *right;
} DDMConditionBinary;

typedef struct {
    DDMCondition *child;
} DDMConditionUnary;

typedef struct {
    gboolean owns_values;
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

DDMCondition *ddm_condition_new_boolean (gboolean           value);
DDMCondition *ddm_condition_new_equal   (DDMConditionValue *left,
                                         DDMConditionValue *right);
DDMCondition *ddm_condition_new_not     (DDMCondition      *child);
DDMCondition *ddm_condition_new_and     (DDMCondition      *left,
                                         DDMCondition      *right);
DDMCondition *ddm_condition_new_or      (DDMCondition      *left,
                                         DDMCondition      *right);

void          ddm_condition_free        (DDMCondition *condition);

DDMCondition *ddm_condition_reduce_source (DDMCondition    *condition,
                                           DDMDataResource *source_resource);
DDMCondition *ddm_condition_reduce_target (DDMCondition    *condition,
                                           DDMDataResource *target_resource);

gboolean      ddm_condition_matches_source(DDMCondition    *condition,
                                           DDMDataResource *source_resource);
gboolean      ddm_condition_matches_target(DDMCondition    *condition,
                                           DDMDataResource *target_resource);

DDMRule      *ddm_rule_new (const char         *target_class_id,
                            const char         *target_property,
                            const char         *source_class_id,
                            DDMDataCardinality  cardinality,
                            gboolean            default_include,
                            const char         *default_children,
                            const char         *condition);

DDMQName           *ddm_rule_get_target_property (DDMRule *rule);
DDMDataCardinality *ddm_rule_get_cardinality     (DDMRule *rule);
const char         *ddm_rule_get_target_class_id (DDMRule *rule);
const char         *ddm_rule_get_source_class_id (DDMRule *rule);

void ddm_rule_free (DDMRule *rule);

DDMCondition *ddm_rule_build_target_condition(DDMRule         *rule,
                                              DDMDataResource *source_resource);
DDMCondition *ddm_rule_build_source_condition(DDMRule         *rule,
                                              DDMDataResource *target_resource);

G_END_DECLS

#endif /* __DDM_RULE_H__ */
