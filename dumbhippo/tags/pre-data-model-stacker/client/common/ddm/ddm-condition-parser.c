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
 *       '-' ? (integer)           # numeric literal
 *     | true | false              # boolean literal
 *     | string                    # string literal
 *     | source                    # source resource itself
 *     | target                    # target resource itself
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

/* Note about floats:
 *
 * Turning on scan_float will break parsing of '.', because of
 * a bug in GScanner (http://bugzilla.gnome.org/show_bug.cgi?id=490235),
 * The hack that I used to get around this (before I decided that I didn't
 * want floats), was to add '.' to cset_identifier_nth, and then in
 * ddm_condition_from_string(), when building the token array, manually
 * check identifiers aginst the forms source.<no_dots> and target.<no_dots>
 * and split them apart into three tokens.
 */
static const GScannerConfig scanner_config = {
    .cset_skip_characters = " \t\n",
    .cset_identifier_first = G_CSET_A_2_Z G_CSET_a_2_z "_",
    .cset_identifier_nth = G_CSET_A_2_Z G_CSET_a_2_z "0123456789_",
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
    .scan_float = FALSE,
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
        if (tokens[i].type == '(') {
            level++;
        } else if (tokens[i].type == ')') {
            level--;
            if (level == 0)
                return i + 1;
        }
    }

    g_warning("Unclosed parentheses in condition expression");

    return -1;
}

/* value := '-' ? integer | string | source.identifier | target.identifier | true | false */
static gboolean
condition_value_from_tokens(ConditionToken    *tokens,
                            int                len,
                            DDMConditionValue *value)
{
    if (len == 1 && tokens[0].type == G_TOKEN_INT) {
        value->type = DDM_CONDITION_VALUE_INTEGER;
        value->u.integer = tokens[0].value.v_int64;
    } else if (len == 2 &&
               tokens[0].type == '-' &&
               tokens[1].type == G_TOKEN_INT) {
        value->type = DDM_CONDITION_VALUE_INTEGER;
        value->u.integer = - tokens[1].value.v_int64;
    } else if (len == 1 && tokens[0].type == G_TOKEN_STRING) {
        value->type = DDM_CONDITION_VALUE_STRING;
        value->u.string = tokens[0].value.v_string;
    } else if (len == 1 && tokens[0].type == SYMBOL_SOURCE) {
        value->type = DDM_CONDITION_VALUE_SOURCE;
    } else if (len == 1 && tokens[0].type == SYMBOL_TARGET) {
        value->type = DDM_CONDITION_VALUE_TARGET;
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

        if (left.type == DDM_CONDITION_VALUE_BOOLEAN)
            return ddm_condition_new_boolean(left.u.boolean);
        else if (left.type == DDM_CONDITION_VALUE_SOURCE_PROPERTY ||
                 left.type == DDM_CONDITION_VALUE_TARGET_PROPERTY)
        {
            right.type = DDM_CONDITION_VALUE_BOOLEAN;
            right.u.boolean = TRUE;
        } else {
            g_warning("Bad type of value for isolated boolean term");
            return NULL;
        }
    }
        
    return ddm_condition_new_equal(&left, &right);

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
        return ddm_condition_new_not(child);
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
            } else if (i == len) {
                break;
            }
        }
        
        if (tokens[i].type == SYMBOL_AND) {
            child = not_condition_from_tokens(tokens + last, i - last);
            if (child == NULL) {
                have_error = TRUE;
                goto out;
            }

            if (result != NULL)
                result = ddm_condition_new_and(result, child);
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
        result = ddm_condition_new_and(result, child);
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
            } else if (i == len) {
                break;
            }
        }
        
        if (tokens[i].type == SYMBOL_OR) {
            child = and_condition_from_tokens(tokens + last, i - last);
            if (child == NULL) {
                have_error = TRUE;
                goto out;
            }

            if (result != NULL)
                result = ddm_condition_new_or(result, child);
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
        result = ddm_condition_new_or(result, child);
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

        case G_TOKEN_INT:
            token.value = scanner->value;
            break;

        case G_TOKEN_STRING:
            token.value.v_string = g_strdup(scanner->value.v_string);
            break;
            
        case G_TOKEN_IDENTIFIER:
            token.value.v_identifier = g_strdup(scanner->value.v_identifier);
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
    case DDM_CONDITION_VALUE_SOURCE:
        g_string_append(result, "source");
        break;
    case DDM_CONDITION_VALUE_TARGET:
        g_string_append(result, "target");
        break;
    case DDM_CONDITION_VALUE_SOURCE_PROPERTY:
        g_string_append(result, "source.");
        g_string_append(result, value->u.string);
        break;
    case DDM_CONDITION_VALUE_TARGET_PROPERTY:
        g_string_append(result, "target.");
        g_string_append(result, value->u.string);
        break;
    case DDM_CONDITION_VALUE_PROPERTY:
        /* Only occurs in partially resolved conditions, so we don't bother
         * stringifying here.
         */
        g_string_append(result,"<property>");
        break;
    case DDM_CONDITION_VALUE_RESOURCE:
        /* Only occurs in partially resolved conditions, so we don't bother
         * stringifying here.
         */
        g_string_append(result,"<resource>");
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
    }
}

static void
condition_to_string_recurse(DDMCondition *condition,
                            GString      *result,
                            gboolean      toplevel)
{
    if (condition->type != DDM_CONDITION_EQUAL &&
        condition->type != DDM_CONDITION_TRUE &&
        condition->type != DDM_CONDITION_FALSE &&
        !toplevel)
    {
        g_string_append_c(result, '(');
    }
    
    switch (condition->type) {
    case DDM_CONDITION_TRUE:
        g_string_append(result, "true");
        break;
    case DDM_CONDITION_FALSE:
        g_string_append(result, "false");
        break;
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

    if (condition->type != DDM_CONDITION_EQUAL &&
        condition->type != DDM_CONDITION_TRUE &&
        condition->type != DDM_CONDITION_FALSE &&
        !toplevel)
    {
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
