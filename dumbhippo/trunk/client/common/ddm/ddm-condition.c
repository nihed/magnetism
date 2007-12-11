/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "ddm-rule.h"

static DDMCondition true_condition = {
    DDM_CONDITION_TRUE,
};

static DDMCondition false_condition = {
    DDM_CONDITION_FALSE,
};

static DDMCondition *
new_condition(DDMConditionType type)
{
    DDMCondition *condition;
    
    condition = g_slice_new(DDMCondition);
    condition->type = type;

    return condition;
}

DDMCondition *
ddm_condition_new_boolean(gboolean value)
{
    if (value)
        return &true_condition;
    else
        return &false_condition;
}

DDMCondition *
ddm_condition_new_equal(DDMConditionValue *left,
                        DDMConditionValue *right)
{
    DDMCondition *condition = new_condition(DDM_CONDITION_EQUAL);
    
    condition->u.equal.owns_values = TRUE;
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

/* Like ddm_condition_new_equal(), but doens't copy the values */
static DDMCondition *
condition_new_equal_temp(DDMConditionValue *left,
                         DDMConditionValue *right)
{
    DDMCondition *condition = new_condition(DDM_CONDITION_EQUAL);
    
    condition->u.equal.owns_values = FALSE;
    condition->u.equal.left = *left;
    condition->u.equal.right = *right;

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
    case DDM_CONDITION_TRUE:
    case DDM_CONDITION_FALSE:
        return;
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
        if (condition->u.equal.owns_values) {
            condition_value_clear(&condition->u.equal.left);
            condition_value_clear(&condition->u.equal.right);
        }
        break;
    }

    g_slice_free(DDMCondition, condition);
}

static void
condition_value_reduce(DDMConditionValue *value,
                       DDMDataResource   *resource,
                       gboolean           is_source)
{
    /* FIXME: DDMConditionValue probably should have the QName so we can use
     * ddm_data_property_get_by_qname; that would mean passing the
     * source/target classes through the parsing code
     */
    if (is_source) {
        if (value->type == DDM_CONDITION_VALUE_SOURCE) {
            value->type = DDM_CONDITION_VALUE_RESOURCE;
            value->u.resource = resource;
        } else if (value->type == DDM_CONDITION_VALUE_SOURCE_PROPERTY) {
            value->type = DDM_CONDITION_VALUE_PROPERTY;
            value->u.property = ddm_data_resource_get_property(resource, value->u.string);
        }
    } else {
        if (value->type == DDM_CONDITION_VALUE_TARGET) {
            value->type = DDM_CONDITION_VALUE_RESOURCE;
            value->u.resource = resource;
        } else if (value->type == DDM_CONDITION_VALUE_TARGET_PROPERTY) {
            value->type = DDM_CONDITION_VALUE_PROPERTY;
            value->u.property = ddm_data_resource_get_property(resource, value->u.string);
        }
    }
}

/* Here's how we do comparisons. The basics are:
 *
 * 1) scalar values compare equal to each other if they have the same value
 * 2) a scalar value compares equal to a list if the scalar has the same
 *    value as one of the members of the list
 * 3) Two lists compare false and generate a warning
 *
 * But:
 *
 * 4) Missing values are treated like scalar false
 * 5) Comparing a scalar boolean value with a list coerces the list to a
 *    boolean value of "is the list empty"
 * 6) a resource is considered equal to a string if the string is equal
 *    to the resource's resource_id
 *
 * In tabular form:
 *
 *               <missing> false  true  <other>    []   [...]
 *  <missing>        T       T      F      F       T      F  
 *  false                    T      F      F       T      F  
 *  true                            T      F       F      T  
 *  <other>                               (1)      F     (2)
 *  []                                            (3)    (3)
 *  [...]                                                (3)
 *
 * (1) A scalar is equal to another scalar, iff they have equal values
 *     and see (6) above
 * (2) A scalar is equal to a list iff it is present in the list
 *     and see (6) above
 * (3) False with a warning
 */
    
static gboolean
compare_properties_1_1(DDMDataValue *left,
                       DDMDataValue *right)
{
    switch (left->type) {
    case DDM_DATA_BOOLEAN:
        if (right->type != DDM_DATA_BOOLEAN)
            return FALSE;
        return !left->u.boolean == !right->u.boolean;
    case DDM_DATA_INTEGER:
        if (right->type == DDM_DATA_INTEGER)
            return left->u.integer == right->u.integer;
        else if (right->type == DDM_DATA_LONG)
            return left->u.integer == right->u.long_;
        else
            return FALSE;
    case DDM_DATA_LONG:
        if (right->type == DDM_DATA_INTEGER)
            return left->u.long_ == right->u.integer;
        else if (right->type == DDM_DATA_LONG)
            return left->u.long_ == right->u.long_;
        else
            return FALSE;
    case DDM_DATA_STRING:
        if (right->type == DDM_DATA_STRING || right->type == DDM_DATA_URL)
            return strcmp(left->u.string, right->u.string) == 0;
        else if (right->type == DDM_DATA_RESOURCE)
            return strcmp(left->u.string, ddm_data_resource_get_resource_id(right->u.resource)) == 0;
        else
            return FALSE;
    case DDM_DATA_URL:
        if (right->type != DDM_DATA_STRING && right->type != DDM_DATA_URL)
            return FALSE;
        return strcmp(left->u.string, right->u.string) == 0;
    case DDM_DATA_RESOURCE:
        if (right->type == DDM_DATA_RESOURCE)
            return left->u.resource == right->u.resource;
        else if (right->type == DDM_DATA_STRING || right->type == DDM_DATA_URL)
            return strcmp(ddm_data_resource_get_resource_id(left->u.resource), right->u.string) == 0;
        else
            return FALSE;
    case DDM_DATA_FLOAT:
    case DDM_DATA_NONE:
    case DDM_DATA_LIST:
        break;
    }

     g_warning("compare_properties_1_1: Unexpected value of type %d", left->type);
     return FALSE;
}

static gboolean
compare_properties_n_1(DDMDataValue *list_value,
                       DDMDataValue *scalar_value)
{
    GSList *l;
    gboolean empty;

    g_return_val_if_fail(list_value->type == DDM_DATA_NONE || DDM_DATA_IS_LIST(list_value->type), FALSE);
    
    empty = list_value->type == DDM_DATA_NONE || list_value->u.list == NULL;

    if (scalar_value->type == DDM_DATA_BOOLEAN)
        return !empty == !!scalar_value->type;
    else if (empty)
        return FALSE;
    else {
        for (l = list_value->u.list; l; l = l->next) {
            DDMDataValue element;
            
            ddm_data_value_get_element(list_value, l, &element);
            if (compare_properties_1_1(&element, scalar_value))
                return TRUE;
        }
    }

    return FALSE;
}

static gboolean
compare_properties(DDMDataProperty *left,
                   DDMDataProperty *right)
{
    DDMDataCardinality left_cardinality;
    DDMDataCardinality right_cardinality;
    DDMDataValue left_value;
    DDMDataValue right_value;

    if (left == NULL) {
        left_cardinality = DDM_DATA_CARDINALITY_1;
        left_value.type = DDM_DATA_BOOLEAN;
        left_value.u.boolean = FALSE;
    } else {
        left_cardinality = ddm_data_property_get_cardinality(left);
        ddm_data_property_get_value(left, &left_value);

        if (left_cardinality != DDM_DATA_CARDINALITY_N && left_value.type == DDM_DATA_NONE) {
            left_value.type = DDM_DATA_BOOLEAN;
            left_value.u.boolean = FALSE;
        }
    }

    if (right == NULL) {
        right_cardinality = DDM_DATA_CARDINALITY_1;
        right_value.type = DDM_DATA_BOOLEAN;
        right_value.u.boolean = FALSE;
    } else {
        right_cardinality = ddm_data_property_get_cardinality(right);
        ddm_data_property_get_value(right, &right_value);
        
        if (right_cardinality != DDM_DATA_CARDINALITY_N && right_value.type == DDM_DATA_NONE) {
            right_value.type = DDM_DATA_BOOLEAN;
            right_value.u.boolean = FALSE;
        }
    }

    if (DDM_DATA_BASE(left_value.type) == DDM_DATA_FLOAT ||
        DDM_DATA_BASE(right_value.type) == DDM_DATA_FLOAT) {
        g_warning("Refusing to compare properties of type float");
        return FALSE;
    }
    
    if (left_cardinality == DDM_DATA_CARDINALITY_N &&
        right_cardinality == DDM_DATA_CARDINALITY_N) {
        g_warning("Don't know how compare two list-valued properties");
        return FALSE;
    } else if (left_cardinality == DDM_DATA_CARDINALITY_N) {
        return compare_properties_n_1(&left_value, &right_value);
    } else if (right_cardinality == DDM_DATA_CARDINALITY_N) {
        return compare_properties_n_1(&right_value, &left_value);
    } else {
        return compare_properties_1_1(&left_value, &right_value);
    }
}

static gboolean
compare_property_literal_1_1(DDMDataValue      *property,
                             DDMConditionValue *literal)
{
    switch (property->type) {
    case DDM_DATA_BOOLEAN:
        if (literal->type != DDM_CONDITION_VALUE_BOOLEAN)
            return FALSE;
        return !property->u.boolean == !literal->u.boolean;
    case DDM_DATA_INTEGER:
        if (literal->type != DDM_CONDITION_VALUE_INTEGER)
            return FALSE;
        return property->u.integer == literal->u.integer;
    case DDM_DATA_LONG:
        if (literal->type != DDM_CONDITION_VALUE_INTEGER)
            return FALSE;
        return property->u.long_ == literal->u.integer;
    case DDM_DATA_STRING:
    case DDM_DATA_URL:
        if (literal->type == DDM_CONDITION_VALUE_STRING)
            return strcmp(property->u.string, literal->u.string) == 0;
        else if (literal->type == DDM_CONDITION_VALUE_RESOURCE)
            return strcmp(property->u.string, ddm_data_resource_get_resource_id(literal->u.resource)) == 0;
        else
            return FALSE;
    case DDM_DATA_RESOURCE:
        if (literal->type == DDM_CONDITION_VALUE_RESOURCE)
            return property->u.resource == literal->u.resource;
        else if (literal->type == DDM_CONDITION_VALUE_STRING)
            return strcmp(ddm_data_resource_get_resource_id(property->u.resource), literal->u.string) == 0;
        else
            return FALSE;
    case DDM_DATA_FLOAT:
        return FALSE;
    case DDM_DATA_NONE:
        break;
    case DDM_DATA_LIST:
        break;
    }

     g_warning("compare_property_literal_1_1: Unexpected value of type %d", property->type);
     return FALSE;
}

static gboolean
compare_property_literal_n_1(DDMDataValue      *list_value,
                             DDMConditionValue *literal)
{
    GSList *l;
    gboolean empty;

    g_return_val_if_fail(list_value->type == DDM_DATA_NONE || DDM_DATA_IS_LIST(list_value->type), FALSE);

    empty = list_value->type == DDM_DATA_NONE || list_value->u.list == NULL;
    
    if (literal->type == DDM_CONDITION_VALUE_BOOLEAN) {
        return !empty == !!literal->u.boolean;
    } else if (empty) {
        return FALSE;
    } else {
        for (l = list_value->u.list; l; l = l->next) {
            DDMDataValue element;
        
            ddm_data_value_get_element(list_value, l, &element);
            if (compare_property_literal_1_1(&element, literal))
                return TRUE;
        }
    }

    return FALSE;
}

static gboolean
compare_property_literal(DDMDataProperty   *property,
                         DDMConditionValue *literal)
{
    DDMDataCardinality property_cardinality;
    DDMDataValue property_value;

    if (property == NULL) {
        property_cardinality = DDM_DATA_CARDINALITY_1;
        property_value.type = DDM_DATA_BOOLEAN;
        property_value.u.boolean = FALSE;
    } else {
        property_cardinality = ddm_data_property_get_cardinality(property);
        ddm_data_property_get_value(property, &property_value);

        if (property_cardinality != DDM_DATA_CARDINALITY_N && property_value.type == DDM_DATA_NONE) {
            property_value.type = DDM_DATA_BOOLEAN;
            property_value.u.boolean = FALSE;
        }
    }

    if (property_cardinality == DDM_DATA_CARDINALITY_N) {
        return compare_property_literal_n_1(&property_value, literal);
    } else {
        return compare_property_literal_1_1(&property_value, literal);
    }
}

static gboolean
compare_literals(DDMConditionValue *left,
                 DDMConditionValue *right)
{
    switch (left->type) {
    case DDM_CONDITION_VALUE_BOOLEAN:
        if (right->type != DDM_CONDITION_VALUE_BOOLEAN)
            return FALSE;
        return !left->u.boolean == !right->u.boolean;
    case DDM_CONDITION_VALUE_INTEGER:
        if (right->type != DDM_CONDITION_VALUE_INTEGER)
            return FALSE;
        return left->u.integer == right->u.integer;
    case DDM_CONDITION_VALUE_STRING:
        if (right->type == DDM_CONDITION_VALUE_STRING)
            return strcmp(left->u.string, right->u.string) == 0;
        else if (right->type == DDM_CONDITION_VALUE_RESOURCE)
            return strcmp(left->u.string, ddm_data_resource_get_resource_id(right->u.resource)) == 0;
        else
            return FALSE;
    case DDM_CONDITION_VALUE_RESOURCE:
        if (right->type == DDM_CONDITION_VALUE_RESOURCE)
            return left->u.resource == right->u.resource;
        else if (right->type == DDM_CONDITION_VALUE_STRING)
            return strcmp(ddm_data_resource_get_resource_id(left->u.resource), right->u.string) == 0;
        else
            return FALSE;
    case DDM_CONDITION_VALUE_SOURCE:
    case DDM_CONDITION_VALUE_TARGET:
    case DDM_CONDITION_VALUE_SOURCE_PROPERTY:
    case DDM_CONDITION_VALUE_TARGET_PROPERTY:
    case DDM_CONDITION_VALUE_PROPERTY:
        break;
    }
    
    g_warning("compare_literals: Unexpected value of type %d", left->type);
    return FALSE;
}

static DDMCondition *
condition_value_compare(DDMConditionValue *left,
                        DDMConditionValue *right)
{
    gboolean result;
    
    if (left->type == DDM_CONDITION_VALUE_SOURCE ||
        left->type == DDM_CONDITION_VALUE_TARGET ||
        left->type == DDM_CONDITION_VALUE_SOURCE_PROPERTY ||
        left->type == DDM_CONDITION_VALUE_TARGET_PROPERTY ||
        right->type == DDM_CONDITION_VALUE_SOURCE ||
        right->type == DDM_CONDITION_VALUE_TARGET ||
        right->type == DDM_CONDITION_VALUE_SOURCE_PROPERTY ||
        right->type == DDM_CONDITION_VALUE_TARGET_PROPERTY)
        return NULL; /* Not fully reduced */

    /* Note that 'literal' here mostly refers to literals in the condition
     * expression, but also to DDM_CONDITION_VALUE_RESOURCE, which is
     * a substitution from 'source' or 'target'. 'literal', is however
     * a better anme than "not a property"
     */
    if (left->type == DDM_CONDITION_VALUE_PROPERTY &&
        right->type == DDM_CONDITION_VALUE_PROPERTY)
        result = compare_properties(left->u.property, right->u.property);
    else if (left->type == DDM_CONDITION_VALUE_PROPERTY)
        result = compare_property_literal(left->u.property, right);
    else if (right->type == DDM_CONDITION_VALUE_PROPERTY)
        result = compare_property_literal(right->u.property, left);
    else
        result = compare_literals(left, right);

    return result ? &true_condition : &false_condition;
}

static DDMCondition *
condition_reduce_internal(DDMCondition    *condition,
                          DDMDataResource *resource,
                          gboolean         is_source)
{
    switch (condition->type) {
    case DDM_CONDITION_TRUE:
    case DDM_CONDITION_FALSE:
        return condition;
    case DDM_CONDITION_OR:
        {
            DDMCondition *left = condition_reduce_internal(condition->u.or.left, resource, is_source);
            DDMCondition *right = condition_reduce_internal(condition->u.or.right, resource, is_source);

            if (left->type == DDM_CONDITION_TRUE || right->type == DDM_CONDITION_TRUE)
                return &true_condition;
            else if (left->type == DDM_CONDITION_FALSE)
                return right;
            else if (right->type == DDM_CONDITION_FALSE)
                return left;
            else
                return ddm_condition_new_or(left, right);
        }
    case DDM_CONDITION_AND:
        {
            DDMCondition *left = condition_reduce_internal(condition->u.and.left, resource, is_source);
            DDMCondition *right = condition_reduce_internal(condition->u.and.right, resource, is_source);

            if (left->type == DDM_CONDITION_FALSE || right->type == DDM_CONDITION_FALSE)
                return &false_condition;
            else if (left->type == DDM_CONDITION_TRUE)
                return right;
            else if (right->type == DDM_CONDITION_TRUE)
                return left;
            else
                return ddm_condition_new_and(left, right);
        }
    case DDM_CONDITION_NOT:
        {
            DDMCondition *child = condition_reduce_internal(condition->u.not.child, resource, is_source);

            if (child->type == DDM_CONDITION_TRUE)
                return &false_condition;
            else if (child->type == DDM_CONDITION_FALSE)
                return &true_condition;
            else
                return ddm_condition_new_not(child);
        }
    case DDM_CONDITION_EQUAL:
        {
            DDMConditionValue left_value = condition->u.equal.left;
            DDMConditionValue right_value = condition->u.equal.right;
            DDMCondition *boolean_condition;

            condition_value_reduce(&left_value, resource, is_source);
            condition_value_reduce(&right_value, resource, is_source);

            boolean_condition = condition_value_compare(&left_value, &right_value);
            if (boolean_condition != NULL)
                return boolean_condition;

            return condition_new_equal_temp(&left_value, &right_value);
        }
    }

    g_assert_not_reached();
    return NULL;
}

DDMCondition *
ddm_condition_reduce_source(DDMCondition    *condition,
                            DDMDataResource *source_resource)
{
    return condition_reduce_internal(condition, source_resource, TRUE);
}

DDMCondition *
ddm_condition_reduce_target(DDMCondition    *condition,
                            DDMDataResource *target_resource)
{
    return condition_reduce_internal(condition, target_resource, FALSE);
}

gboolean
ddm_condition_matches_source(DDMCondition    *condition,
                             DDMDataResource *source_resource)
{
    DDMCondition *reduced = ddm_condition_reduce_source(condition, source_resource);
    if (reduced->type == DDM_CONDITION_TRUE)
        return TRUE;
    else if (reduced->type == DDM_CONDITION_FALSE)
        return FALSE;
    else {
        g_warning("ddm_condition_matches_source(): condition still had target dependency");
        ddm_condition_free(reduced);
        return FALSE;
    }
}

gboolean
ddm_condition_matches_target(DDMCondition    *condition,
                             DDMDataResource *target_resource)
{
    DDMCondition *reduced = ddm_condition_reduce_target(condition, target_resource);
    if (reduced->type == DDM_CONDITION_TRUE)
        return TRUE;
    else if (reduced->type == DDM_CONDITION_FALSE)
        return FALSE;
    else {
        g_warning("ddm_condition_matches_target(): condition still had source dependency");
        ddm_condition_free(reduced);
        return FALSE;
    }
}
