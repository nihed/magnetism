/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-rule.h"

DDMRule *
ddm_rule_new (const char         *target_class_id,
	      const char         *target_property_uri,
	      const char         *source_class_id,
	      DDMDataCardinality  cardinality,
	      gboolean            default_include,
	      const char         *default_children_str,
	      const char         *condition_str)
{
    DDMQName *target_property;
    DDMCondition *condition;
    DDMDataFetch *default_children;
    DDMRule *rule;

    target_property = ddm_qname_from_uri(target_property_uri);
    if (target_property == NULL) /* Will already have warned */
        return NULL;
    
    condition = ddm_condition_from_string(condition_str);
    if (condition == NULL) /* Will already have warned */
        return NULL;

    if (default_children_str) {
        default_children = ddm_data_fetch_from_string(default_children_str);
        if (default_children == NULL) /* Will have already warned */
            return NULL;
    } else {
        default_children = NULL;
    }

    rule = g_new0(DDMRule, 1);
    rule->target_class_id = g_strdup(target_class_id);
    rule->target_property= target_property;
    rule->source_class_id = g_strdup(source_class_id);
    rule->cardinality = cardinality;
    rule->default_children = default_children;
    rule->condition = condition;

    return rule;
}

void
ddm_rule_free (DDMRule *rule)
{
    g_free(rule->target_class_id);
    g_free(rule->source_class_id);
    if (rule->default_children)
        ddm_data_fetch_unref(rule->default_children);
    ddm_condition_free(rule->condition);
    
    g_free(rule);
}

DDMCondition *
ddm_rule_build_target_condition(DDMRule         *rule,
                                DDMDataResource *source_resource)
{
    return ddm_condition_reduce_source(rule->condition, source_resource);
}

DDMCondition *
ddm_rule_build_source_condition(DDMRule         *rule,
                                DDMDataResource *target_resource)
{
    return ddm_condition_reduce_target(rule->condition, target_resource);
}
