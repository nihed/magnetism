/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "ddm-rule.h"

DDMCondition *
new_condition(DDMConditionType type)
{
    DDMCondition *condition;
    
    condition = g_slice_new(DDMCondition);
    condition->type = type;

    return condition;
}

DDMCondition *
ddm_condition_new_equal(DDMConditionValue *left,
                        DDMConditionValue *right)
{
    DDMCondition *condition = new_condition(DDM_CONDITION_EQUAL);
    condition->u.equal.left = *left;
    condition->u.equal.right = *right;

    if (left->type == DDM_CONDITION_VALUE_STRING ||
        left->type == DDM_CONDITION_VALUE_SOURCE_PROPERTY ||
        left->type == DDM_CONDITION_VALUE_TARGET_PROPERTY)
        condition->u.equal.left.u.string = g_strdup(condition->u.equal.left.u.string);
    
    if (right->type == DDM_CONDITION_VALUE_STRING ||
        right->type == DDM_CONDITION_VALUE_SOURCE_PROPERTY ||
        right->type == DDM_CONDITION_VALUE_TARGET_PROPERTY)
        condition->u.equal.right.u.string = g_strdup(condition->u.equal.right.u.string);

    return condition;
}

DDMCondition *
ddm_condition_new_not(DDMCondition *child)
{
    DDMCondition *condition = new_condition(DDM_CONDITION_NOT);
    
    condition->u.not.child = child;

    return condition;
}

DDMCondition *
ddm_condition_new_and(DDMCondition *left,
                      DDMCondition *right)
{
    DDMCondition *condition = new_condition(DDM_CONDITION_AND);
    
    condition->u.and.left = left;
    condition->u.and.right = right;

    return condition;
}

DDMCondition *
ddm_condition_new_or(DDMCondition *left,
                     DDMCondition *right)
{
    DDMCondition *condition = new_condition(DDM_CONDITION_OR);
    
    condition->u.or.left = left;
    condition->u.or.right = right;

    return condition;
}

static void
condition_value_clear(DDMConditionValue *value)
{
    if (value->type == DDM_CONDITION_VALUE_STRING ||
        value->type == DDM_CONDITION_VALUE_SOURCE_PROPERTY ||
        value->type == DDM_CONDITION_VALUE_TARGET_PROPERTY)
        g_free(value->u.string);
}

void
ddm_condition_free(DDMCondition *condition)
{
    switch (condition->type) {
    case DDM_CONDITION_OR:
        ddm_condition_free(condition->u.or.left);
        ddm_condition_free(condition->u.or.right);
        break;
    case DDM_CONDITION_AND:
        ddm_condition_free(condition->u.and.left);
        ddm_condition_free(condition->u.and.right);
        break;
    case DDM_CONDITION_NOT:
        ddm_condition_free(condition->u.not.child);
        break;
    case DDM_CONDITION_EQUAL:
        condition_value_clear(&condition->u.equal.left);
        condition_value_clear(&condition->u.equal.right);
        break;
    }

    g_slice_free(DDMCondition, condition);
}
