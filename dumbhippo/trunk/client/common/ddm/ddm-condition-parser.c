/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "ddm-rule.h"

/* This file implements a parser for a simple rule-definition language
 * that is used to define what "source" resources should be included in
 * a "target" property defined by a rule.
 *
 * The BNF is:
 *   expression := or_expression
 *   or_expression := and_expression ( 'or' and_expression )
 *   not_expression := 'not' ? term
 *   term :=
 *      '(' or_expression ')'
 *     | value = value
 *     | value                     # same as value = true
 *   value :=
 *       '-' ? (integer | float)   # numeric literal
 *     | true | false              # boolean literal
 *     | string                    # string literal
 *     | source.identifier         # property on a source resource
 *     | target.identifier         # property on the target resource
 *
 * The syntax is vaguely SQL-like, except that it's case-sensitive and
 * the keywords must be lowercase.
 * 
 * The main justification for using '=' rather than '==' and for using
 * 'and'/'or'/'not' rather than '&&'/'||/'!', is that multi-character
 * operators are a pain to do with GScanner.
 *
 * The main justification for using GScanner and a hand-rolled parser
 * rather than lex and yacc, is that a) lex and yacc suck for all the
 * usual reasons b) I didn't want to pull them into the build process.
 *
 * The approach of the parser is to first tokenize the entire string into
 * an array of tokens, and then do recursive descent on the token array,
 * which saves thinking about lookahead and so forth. Efficiency isn't
 * a big deal, since we only expect to parse a small number of rule strings
 * at startup time.
 */
typedef struct {
    GTokenType type;
    GTokenValue value;
} ConditionToken;

/* GScanner configuration */;
static const GScannerConfig scanner_config = {
    .cset_skip_characters = " \t\n",
    .cset_identifier_first = G_CSET_A_2_Z G_CSET_a_2_z "_",
    /* See comment in ddm_condition_to_string() about the presence of . here */
    .cset_identifier_nth = G_CSET_A_2_Z G_CSET_a_2_z "0123456789_.",
    .cpair_comment_single = NULL,
    .case_sensitive = TRUE,
    .skip_comment_multi = TRUE, /* C style comments */
    .skip_comment_single = FALSE,
    .scan_comment_multi = TRUE,
    .scan_identifier = TRUE,
    .scan_identifier_1char = TRUE,
    .scan_identifier_NULL = FALSE,
    .scan_symbols = TRUE,
    .scan_binary = FALSE,
    .scan_octal = TRUE,
    .scan_float = TRUE,
    .scan_hex = TRUE,
    .scan_hex_dollar = FALSE,
    .scan_string_sq = TRUE,
    .scan_string_dq = TRUE,
    .numbers_2_int = TRUE,
    .int_2_float = FALSE,
    .identifier_2_string = FALSE,
    .char_2_token = TRUE,
    .symbol_2_token = TRUE,
    .scope_0_fallback = FALSE,
    .store_int64 = TRUE
};

typedef enum {
    SYMBOL_INVALID = G_TOKEN_LAST,
    SYMBOL_AND,
    SYMBOL_FALSE,
    SYMBOL_NOT,
    SYMBOL_OR,
    SYMBOL_SOURCE,
    SYMBOL_TRUE,
    SYMBOL_TARGET
} ConditionSymbol;

static const struct {
    const char *name;
    ConditionSymbol token;
} symbols[] = {
    { "and", SYMBOL_AND }, 
    { "false", SYMBOL_FALSE }, 
    { "or", SYMBOL_OR },
    { "not", SYMBOL_NOT },
    { "source", SYMBOL_SOURCE },
    { "target", SYMBOL_TARGET },
    { "true", SYMBOL_TRUE }
};

static DDMCondition *or_condition_from_tokens(ConditionToken *tokens,
                                              int             len);

static DDMCondition *
new_condition(DDMConditionType type)
{
    DDMCondition *condition;
    
    condition = g_slice_new(DDMCondition);
    condition->type = type;

    return condition;
}

static DDMCondition *
new_equal_condition(DDMConditionValue *left,
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

static DDMCondition *
new_not_condition(DDMCondition *child)
{
    DDMCondition *condition = new_condition(DDM_CONDITION_NOT);
    
    condition->u.not.child = child;

    return condition;
}

static DDMCondition *
new_and_condition(DDMCondition *left,
                  DDMCondition *right)
{
    DDMCondition *condition = new_condition(DDM_CONDITION_AND);
    
    condition->u.and.left = left;
    condition->u.and.right = right;

    return condition;
}

static DDMCondition *
new_or_condition(DDMCondition *left,
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

static int
skip_parens(ConditionToken *tokens,
            int             start,
            int             len)
{
    int i;
    int level;

    g_return_val_if_fail(start < len, -1);
    g_return_val_if_fail(tokens[start].type == '(', -1);

    level = 1;
    for (i = start + 1; i < len; i++) {
        if (tokens[start].type == '(') {
            level++;
        } else if (tokens[start].type == ')') {
            level--;
            if (level == 0)
                return i + 1;
        }
    }

    g_warning("Unclosed parentheses in condition expression");

    return -1;
}

/* value := '-' ? (integer | float) | string | source.identifier | target.identifier | true | false */
static gboolean
condition_value_from_tokens(ConditionToken    *tokens,
                            int                len,
                            DDMConditionValue *value)
{
    if (len == 1 && tokens[0].type == G_TOKEN_INT) {
        value->type = DDM_CONDITION_VALUE_INTEGER;
        value->u.integer = tokens[0].value.v_int64;
    } else if (len == 1 && tokens[0].type == G_TOKEN_FLOAT) {
        value->type = DDM_CONDITION_VALUE_FLOAT;
        value->u.float_ = tokens[0].value.v_float;
    } else if (len == 2 &&
               tokens[0].type == '-' &&
               tokens[1].type == G_TOKEN_INT) {
        value->type = DDM_CONDITION_VALUE_INTEGER;
        value->u.integer = - tokens[1].value.v_int64;
    } else if (len == 2 &&
               tokens[0].type == '-' &&
               tokens[1].type == G_TOKEN_FLOAT) {
        value->type = DDM_CONDITION_VALUE_FLOAT;
        value->u.float_ = - tokens[1].value.v_float;
    } else if (len == 1 && tokens[0].type == G_TOKEN_STRING) {
        value->type = DDM_CONDITION_VALUE_STRING;
        value->u.string = tokens[0].value.v_string;
    } else if (len == 3 &&
               tokens[0].type == SYMBOL_SOURCE &&
               tokens[1].type == '.' &&
               tokens[2].type == G_TOKEN_IDENTIFIER) {
        value->type = DDM_CONDITION_VALUE_SOURCE_PROPERTY;
        value->u.string = tokens[2].value.v_identifier;
    } else if (len == 3 &&
               tokens[0].type == SYMBOL_TARGET &&
               tokens[1].type == '.' &&
               tokens[2].type == G_TOKEN_IDENTIFIER) {
        value->type = DDM_CONDITION_VALUE_TARGET_PROPERTY;
        value->u.string = tokens[2].value.v_identifier;
    } else if (len == 1 && tokens[0].type == SYMBOL_TRUE) {
        value->type = DDM_CONDITION_VALUE_BOOLEAN;
        value->u.boolean = TRUE;
    } else if (len == 1 && tokens[0].type == SYMBOL_FALSE) {
        value->type = DDM_CONDITION_VALUE_BOOLEAN;
        value->u.boolean = FALSE;
    } else {
        g_warning("Syntax error parsing condition expression");
        return FALSE;
    }

    return TRUE;
}

/* term := '(' or_expression ')' | value '=' value | value */
static DDMCondition *
term_from_tokens(ConditionToken *tokens,
                 int             len)
{
    DDMConditionValue left;
    DDMConditionValue right;
    int i;
    int equal_pos;
    
    if (len > 0 && tokens[0].type == '(') {
        int new_pos = skip_parens(tokens, 0, len);
        if (new_pos != len) {
            g_warning("Syntax error parsing condition expression");
            return NULL;
        }

        return or_condition_from_tokens(tokens + 1, len - 2);
    }

    equal_pos = -1;
    for (i = 0; i < len - 1; i++) {
        if (tokens[i].type == '=') {
            equal_pos = i;
            break;
        }
    }

    if (equal_pos != -1) {
        if (!condition_value_from_tokens(tokens, equal_pos, &left) ||
            !condition_value_from_tokens(tokens + equal_pos + 1, len - equal_pos - 1, &right))
            return NULL;
    } else {
        if (!condition_value_from_tokens(tokens, len, &left))
            return NULL;

        if (!(left.type == DDM_CONDITION_VALUE_SOURCE_PROPERTY ||
              left.type == DDM_CONDITION_VALUE_TARGET_PROPERTY ||
              left.type == DDM_CONDITION_VALUE_BOOLEAN))
        {
            g_warning("Bad type of value for isolated boolean term");
            return NULL;
        }

        right.type = DDM_CONDITION_VALUE_BOOLEAN;
        right.u.boolean = TRUE;
    }
        
    return new_equal_condition(&left, &right);

}

/* not_expression := 'not' ? term */
static DDMCondition *
not_condition_from_tokens(ConditionToken *tokens,
                          int             len)
{
    DDMCondition *child;
    int start;
    
    if (len > 0 && tokens[0].type == SYMBOL_NOT) {
        start = 1;
    } else {
        start = 0;
    }
    
    child = term_from_tokens(tokens + start, len - start);
    if (child == NULL)
        return NULL;

    if (tokens[0].type == SYMBOL_NOT) {
        return new_not_condition(child);
    } else {
        return child;
    }
}

/* and_expression := not_expression ( 'and' not_expression ) * */
static DDMCondition *
and_condition_from_tokens(ConditionToken *tokens,
                          int             len)
{
    DDMCondition *result = NULL;
    gboolean have_error = FALSE;
    int i;
    int last = 0;
    DDMCondition *child;
    
    for (i = 0; i < len; i++) {
        if (tokens[i].type == '(') {
            i = skip_parens(tokens, i, len);
            if (i == -1) {
                have_error = TRUE;
                goto out;
            }
        }
        
        if (tokens[i].type == SYMBOL_AND) {
            child = not_condition_from_tokens(tokens + last, i - last);
            if (child == NULL) {
                have_error = TRUE;
                goto out;
            }

            if (result != NULL)
                result = new_and_condition(result, child);
            else
                result = child;
            
            last = i + 1;
        }
    }
    
    child = not_condition_from_tokens(tokens + last, i - last);
    if (child == NULL) {
        have_error = TRUE;
        goto out;
    }

    if (result != NULL)
        result = new_and_condition(result, child);
    else
        result = child;

 out:
    if (have_error && result) {
        ddm_condition_free(result);
        result = NULL;
    }

    return result;
}

/* or_expression := and_expression ( 'or' and_expression ) * */
static DDMCondition *
or_condition_from_tokens(ConditionToken *tokens,
                         int             len)
{
    DDMCondition *result = NULL;
    gboolean have_error = FALSE;
    int i;
    int last = 0;
    DDMCondition *child;
    
    for (i = 0; i < len; i++) {
        if (tokens[i].type == '(') {
            i = skip_parens(tokens, i, len);
            if (i == -1) {
                have_error = TRUE;
                goto out;
            }
        }
        
        if (tokens[i].type == SYMBOL_OR) {
            child = and_condition_from_tokens(tokens + last, i - last);
            if (child == NULL) {
                have_error = TRUE;
                goto out;
            }

            if (result != NULL)
                result = new_or_condition(result, child);
            else
                result = child;
            
            last = i + 1;
        }
    }
    
    child = and_condition_from_tokens(tokens + last, i - last);
    if (child == NULL) {
        have_error = TRUE;
        goto out;
    }

    if (result != NULL)
        result = new_or_condition(result, child);
    else
        result = child;

 out:
    if (have_error && result) {
        ddm_condition_free(result);
        result = NULL;
    }

    return result;
}

static DDMCondition *
condition_from_tokens(ConditionToken *tokens,
                      int             len)
{
    return or_condition_from_tokens(tokens, len);
}

DDMCondition *
ddm_condition_from_string (const char *str)
{
    GScanner *scanner;
    GArray *tokens;
    DDMCondition *condition = NULL;
    size_t i;
    gboolean done;

    scanner = g_scanner_new(&scanner_config);
    
    for (i = 0; i < G_N_ELEMENTS(symbols); i++)
        g_scanner_scope_add_symbol(scanner, 0, symbols[i].name, GINT_TO_POINTER(symbols[i].token));

    tokens = g_array_new(FALSE, FALSE, sizeof(ConditionToken));

    g_scanner_input_text(scanner, str, strlen(str));

    done = FALSE;
    
    while (TRUE) {
        ConditionToken token;

        token.type = g_scanner_get_next_token(scanner);
        memset(&token.value, 0, sizeof(token.value));
        
        switch (token.type) {
        case G_TOKEN_EOF:
            goto done_scanning;
            
        case G_TOKEN_ERROR:
            g_scanner_unexp_token(scanner, G_TOKEN_EOF, NULL, NULL, NULL,
                                  "Error parsing rule condition", TRUE);
            goto error;
            
        case '(':
        case ')':
        case '.':
        case '=':
        case '-':
        case SYMBOL_AND:
        case SYMBOL_OR:
        case SYMBOL_NOT:
        case SYMBOL_SOURCE:
        case SYMBOL_TARGET:
        case SYMBOL_TRUE:
        case SYMBOL_FALSE:
            break;

        case G_TOKEN_FLOAT:
        case G_TOKEN_INT:
            token.value = scanner->value;
            break;

        case G_TOKEN_STRING:
            token.value.v_string = g_strdup(scanner->value.v_string);
            break;
            
        case G_TOKEN_IDENTIFIER:
            /* A bug in GScanner (http://bugzilla.gnome.org/show_bug.cgi?id=490235)
             * means that we can't parse <symbol>.<identifier> in that way, so
             * we add . to cst_identifier_nth, and then split things apart ourselves.
             * This does prohibit people from writing 'source . <propertyName>', but
             * that's unusual in any case.
             */
            {
                const char *s = scanner->value.v_identifier;
                const char *dot = strchr(s, '.');

                if (dot != NULL) {
                    ConditionToken tmp_token;
                    memset(&tmp_token.value, 0, sizeof(tmp_token.value));
                    
                    if (strchr(dot + 1, '.') != NULL ||
                        dot[1] == '\0' ||
                        (dot[1] >= '0' && dot[1] <= '9'))
                    {
                        g_warning("Bad property path %s", s);
                        goto error;
                    }

                    if (g_str_has_prefix(s, "source.")) {
                        tmp_token.type = SYMBOL_SOURCE;
                        g_array_append_val(tokens, tmp_token);
                    } else if (g_str_has_prefix(s, "target.")) {
                        tmp_token.type = SYMBOL_TARGET;
                        g_array_append_val(tokens, tmp_token);
                    } else {
                        g_warning("Bad property path %s", s);
                        goto error;
                    }

                    tmp_token.type = '.';
                    g_array_append_val(tokens, tmp_token);

                    token.value.v_identifier = g_strdup(dot + 1);
                    
                } else {
                    token.value.v_identifier = g_strdup(s);
                }
                
            }
            break;
            
        default:
            g_scanner_unexp_token(scanner, G_TOKEN_NONE, NULL, NULL, NULL,
                                  "Error parsing rule condition", TRUE);
            goto error;
        }

        g_array_append_val(tokens, token);
    }
 done_scanning:

    condition = condition_from_tokens((ConditionToken *)tokens->data, tokens->len);

 error:
    g_scanner_destroy(scanner);

    for (i = 0; i < tokens->len; i++) {
        ConditionToken *token = &g_array_index(tokens, ConditionToken, i);
        switch (token->type) {
        case G_TOKEN_STRING:
            g_free(token->value.v_string);
            break;
        case G_TOKEN_IDENTIFIER:
            g_free(token->value.v_identifier);
            break;
        default:
            break;
        }
    }
    g_array_free(tokens, TRUE);
    
    return condition;
}

static void
condition_value_to_string(DDMConditionValue *value,
                          GString           *result)
{
    switch (value->type) {
    case DDM_CONDITION_VALUE_SOURCE_PROPERTY:
        g_string_append(result, "source.");
        g_string_append(result, value->u.string);
        break;
    case DDM_CONDITION_VALUE_TARGET_PROPERTY:
        g_string_append(result, "target.");
        g_string_append(result, value->u.string);
        break;
    case DDM_CONDITION_VALUE_STRING:
        {
            char *p;
            g_string_append_c(result, '"');
            for (p = value->u.string; *p; p++) {
                if (*p == '"')
                    g_string_append(result, "\\\"");
                else if (*p == '\\')
                    g_string_append(result, "\\\\");
                else
                    g_string_append_c(result, *p);
            }
            g_string_append_c(result, '"');
        }
        break;
    case DDM_CONDITION_VALUE_BOOLEAN:
        if (value->u.boolean)
            g_string_append(result, "true");
        else
            g_string_append(result, "false");
        break;
    case DDM_CONDITION_VALUE_INTEGER:
        g_string_append_printf(result, "%" G_GINT64_MODIFIER "d", value->u.integer);
        break;
    case DDM_CONDITION_VALUE_FLOAT:
        g_string_append_printf(result, "%g", value->u.float_);
        break;
    }
}

static void
condition_to_string_recurse(DDMCondition *condition,
                            GString      *result,
                            gboolean      toplevel)
{
    if (condition->type != DDM_CONDITION_EQUAL && !toplevel) {
        g_string_append_c(result, '(');
    }
    
    switch (condition->type) {
    case DDM_CONDITION_OR:
        condition_to_string_recurse(condition->u.or.left, result, FALSE);
        g_string_append(result, " or ");
        condition_to_string_recurse(condition->u.or.right, result, FALSE);
        break;
    case DDM_CONDITION_AND:
        condition_to_string_recurse(condition->u.and.left, result, FALSE);
        g_string_append(result, " and ");
        condition_to_string_recurse(condition->u.and.right, result, FALSE);
        break;
    case DDM_CONDITION_NOT:
        g_string_append(result, "not ");
        condition_to_string_recurse(condition->u.not.child, result, FALSE);
        break;
    case DDM_CONDITION_EQUAL:
        condition_value_to_string(&condition->u.equal.left, result);
        g_string_append(result, " = ");
        condition_value_to_string(&condition->u.equal.right, result);
        break;
    }

    if (condition->type != DDM_CONDITION_EQUAL && !toplevel) {
        g_string_append_c(result, ')');
    }
}

char *
ddm_condition_to_string (DDMCondition *condition)
{
    GString *result = g_string_new(NULL);

    condition_to_string_recurse(condition, result, TRUE);

    return g_string_free(result, FALSE);
}
