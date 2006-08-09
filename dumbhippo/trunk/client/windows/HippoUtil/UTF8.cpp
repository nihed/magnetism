/* UTF8.c - Operations on UTF-8 strings.
 * 
 * Based on gutf8.c from GLib, which was in turn based on libcharset
 *
 * Copyright (C) 1999 Tom Tromey
 * Copyright (C) 2000 Red Hat, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

#ifdef WIN32
#include <stdafx-hippoutil.h>
#endif

#include <stdint.h>
#include <new>
#include <string>

typedef unsigned char guchar;
typedef unsigned int guint;
typedef uint32_t gunichar;

typedef uint16_t WCHAR;

#define REPLACEMENT_CHARACTER 0xFFFD

class HippoValidationException : public std::exception {
public:
  HippoValidationException(const char *what) : what_(what) {}
  virtual ~HippoValidationException() throw() {}
  virtual const char *what() const throw() {
    return what_.c_str();
  }

private:
  std::string what_;
};

#define utf8_next_char(p) (char *)((p) + utf8_skip[*(guchar *)(p)])

#define G_STMT_START do
#define G_STMT_END while (0)
#define G_UNLIKELY(expr) expr

#define g_return_val_if_fail(cond, val)         \
  G_STMT_START {                                \
    if (!(cond))                                \
       return (val);                            \
  } G_STMT_END

#define UTF8_LENGTH(Char)              \
  ((Char) < 0x80 ? 1 :                 \
   ((Char) < 0x800 ? 2 :               \
    ((Char) < 0x10000 ? 3 :            \
     ((Char) < 0x200000 ? 4 :          \
      ((Char) < 0x4000000 ? 5 : 6)))))
   

#define UNICODE_VALID(Char)                   \
    ((Char) < 0x110000 &&                     \
     (((Char) & 0xFFFFF800) != 0xD800) &&     \
     ((Char) < 0xFDD0 || (Char) > 0xFDEF) &&  \
     ((Char) & 0xFFFE) != 0xFFFE)
   
     
static const char utf8_skip[256] = {
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
  2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
  3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,6,6,1,1
};

/**
 * utf8_find_next_char:
 * @p: a pointer to a position within a UTF-8 encoded string
 * @end: a pointer to the end of the string, or %NULL to indicate
 *        that the string is nul-terminated
 *
 * Finds the start of the next UTF-8 character in the string after @p.
 *
 * @p does not have to be at the beginning of a UTF-8 character. No check
 * is made to see if the character found is actually valid other than
 * it starts with an appropriate byte.
 * 
 * Return value: a pointer to the found character; if no character
 *   was found, returns end (a nul is a valid characterstart , so
 *   we'll always find a character in the nul-terminated case.)
 **/
static const char *
utf8_find_next_char (const char *p,
                     const char *end)
{
  if (*p)
    {
      if (end)
        for (++p; p < end && (*p & 0xc0) == 0x80; ++p)
          ;
      else
        for (++p; (*p & 0xc0) == 0x80; ++p)
          ;
    }

  return p;
}

/**
 * unichar_to_utf8:
 * @c: a Unicode character code
 * @outbuf: output buffer, must have at least 6 bytes of space.
 *       If %NULL, the length will be computed and returned
 *       and nothing will be written to @outbuf.
 * 
 * Converts a single character to UTF-8.
 * 
 * Return value: number of bytes written
 **/
static int
unichar_to_utf8 (gunichar  c,
                 char     *outbuf)
{
  guint len = 0;    
  int first;
  int i;

  if (c < 0x80)
    {
      first = 0;
      len = 1;
    }
  else if (c < 0x800)
    {
      first = 0xc0;
      len = 2;
    }
  else if (c < 0x10000)
    {
      first = 0xe0;
      len = 3;
    }
   else if (c < 0x200000)
    {
      first = 0xf0;
      len = 4;
    }
  else if (c < 0x4000000)
    {
      first = 0xf8;
      len = 5;
    }
  else
    {
      first = 0xfc;
      len = 6;
    }

  if (outbuf)
    {
      for (i = len - 1; i > 0; --i)
        {
          outbuf[i] = (c & 0x3f) | 0x80;
          c >>= 6;
        }
      outbuf[0] = c | first;
    }

  return len;
}

/* Like utf8_get_char, but take a maximum length
 * and return (gunichar)-2 on incomplete trailing character
 */
static inline gunichar
utf8_get_char_extended (const     char *p,
                        intptr_t        max_len)  
{
  intptr_t i, len;
  gunichar wc = (guchar) *p;

  if (wc < 0x80)
    {
      return wc;
    }
  else if (wc < 0xc0)
    {
      return (gunichar)-1;
    }
  else if (wc < 0xe0)
    {
      len = 2;
      wc &= 0x1f;
    }
  else if (wc < 0xf0)
    {
      len = 3;
      wc &= 0x0f;
    }
  else if (wc < 0xf8)
    {
      len = 4;
      wc &= 0x07;
    }
  else if (wc < 0xfc)
    {
      len = 5;
      wc &= 0x03;
    }
  else if (wc < 0xfe)
    {
      len = 6;
      wc &= 0x01;
    }
  else
    {
      return (gunichar)-1;
    }
  
  if (max_len >= 0 && len > max_len)
    {
      for (i = 1; i < max_len; i++)
        {
          if ((((guchar *)p)[i] & 0xc0) != 0x80)
            return (gunichar)-1;
        }
      return (gunichar)-2;
    }

  for (i = 1; i < len; ++i)
    {
      gunichar ch = ((guchar *)p)[i];
      
      if ((ch & 0xc0) != 0x80)
        {
          if (ch)
            return (gunichar)-1;
          else
            return (gunichar)-2;
        }

      wc <<= 6;
      wc |= (ch & 0x3f);
    }

  if (UTF8_LENGTH(wc) != len)
    return (gunichar)-1;
  
  return wc;
}

#define SURROGATE_VALUE(h,l) (((h) - 0xd800) * 0x400 + (l) - 0xdc00 + 0x10000)

/**
 * utf16_to_utf8:
 * @str: a UTF-16 encoded string
 * @len: the maximum length (number of <type>gunichar2</type>) of @str to use. 
 *       If @len < 0, then the string is terminated with a 0 character.
 *
 * Convert a string from UTF-16 to UTF-8. The result will be
 * terminated with a 0 byte.
 *
 * Note that the input is expected to be already in native endianness,
 * an initial byte-order-mark character is not handled specially.
 * g_convert() can be used to convert a byte buffer of UTF-16 data of 
 * ambiguous endianess.
 * 
 * Return value: a pointer to a newly allocated UTF-8 string.
 *               This value must be freed with free(). If an
 *               error occurs, %NULL will be returned and
 *               @error set.
 **/
char *
hippo_utf16_to_utf8 (const WCHAR  *str,
                     long          len) throw (std::bad_alloc)
{
  const WCHAR *in;
  char *out;
  char *result = NULL;
  int n_bytes;
  gunichar high_surrogate;

  g_return_val_if_fail (str != 0, NULL);

  n_bytes = 0;
  in = str;
  high_surrogate = 0;
  while ((len < 0 || in - str < len) && *in)
    {
      WCHAR c = *in;
      gunichar wc;

      if (c >= 0xdc00 && c < 0xe000) /* low surrogate */
        {
          if (high_surrogate)
            {
              wc = SURROGATE_VALUE (high_surrogate, c);
              high_surrogate = 0;
            }
          else
            wc = REPLACEMENT_CHARACTER;
        }
      else
        {
          if (high_surrogate)
            {
              n_bytes += UTF8_LENGTH (REPLACEMENT_CHARACTER);
              high_surrogate = 0;
            }

          if (c >= 0xd800 && c < 0xdc00) /* high surrogate */
            {
              high_surrogate = c;
              goto next1;
            }
          else
            wc = c;
        }

      n_bytes += UTF8_LENGTH (wc);

    next1:
      in++;
    }

  if (high_surrogate)
    n_bytes += UTF8_LENGTH (REPLACEMENT_CHARACTER);
  
  result = new char[n_bytes + 1];
  if (!result)
    throw std::bad_alloc();
  
  high_surrogate = 0;
  out = result;
  in = str;
  while (out < result + n_bytes)
    {
      WCHAR c = *in;
      gunichar wc;

      if (c >= 0xdc00 && c < 0xe000) /* low surrogate */
        {
          if (high_surrogate)
            {
              wc = SURROGATE_VALUE (high_surrogate, c);
              high_surrogate = 0;
            }
          else
            wc = REPLACEMENT_CHARACTER;
        }
      else
        {
          if (high_surrogate)
            {
              out += unichar_to_utf8 (REPLACEMENT_CHARACTER, out);
              high_surrogate = 0;
            }

          if (c >= 0xd800 && c < 0xdc00) /* high surrogate */
            {
              high_surrogate = c;
              goto next2;
            }
          else
            wc = c;
        }

      out += unichar_to_utf8 (wc, out);

    next2:
      in++;
    }
  
  if (high_surrogate)
    out += unichar_to_utf8 (REPLACEMENT_CHARACTER, out);
  
  *out = '\0';

  return result;
}

static int
utf8_to_utf16_count(const char *str,
                    long        len)
{
  int n16;
  const char *in;

  in = str;
  n16 = 0;
  while ((len < 0 || str + len - in > 0) && *in)
    {
      gunichar wc = utf8_get_char_extended (in, len < 0 ? 6 : str + len - in);
      const char *next;
      
      if (wc & 0x80000000)
        {
          wc = REPLACEMENT_CHARACTER;
          next = utf8_find_next_char(in, len < 0 ? NULL : str + len);
        }
      else
        {
          if (!UNICODE_VALID(wc))
            wc = REPLACEMENT_CHARACTER;
          
          next = utf8_next_char(in);
        }

      if (wc < 0x10000)
        {
          n16 += 1;
        }
      else
        {
          n16 += 2;
        }
      
      in = next;
    }

  return n16;
}

static int
utf8_to_utf16_output(const char *str,
                     long        len,
                     int         n16,
                     WCHAR      *buffer)
{
  const char *in;
  int i;

  in = str;
  for (i = 0; i < n16;)
    {
      gunichar wc = utf8_get_char_extended (in, len < 0 ? 6 : str + len - in);
      const char *next;
      
      if (wc & 0x80000000)
        {
          wc = REPLACEMENT_CHARACTER;
          next = utf8_find_next_char(in, str + len);
        }
      else
        {
          if (!UNICODE_VALID(wc))
            wc = REPLACEMENT_CHARACTER;
          
          next = utf8_next_char(in);
        }

      if (wc < 0x10000)
        {
          buffer[i++] = wc;
        }
      else
        {
          buffer[i++] = (wc - 0x10000) / 0x400 + 0xd800;
          buffer[i++] = (wc - 0x10000) % 0x400 + 0xdc00;
        }
      
      in = next;
    }

  return n16;
}

/**
 * utf8_to_utf16:
 * @str: a UTF-8 encoded string
 * @len: the maximum length (number of characters) of @str to use. 
 *       If @len < 0, then the string is nul-terminated.
 *
 * Convert a string from UTF-8 to UTF-16. A 0 character will be
 * added to the result after the converted text. Invalid sequences
 * are replaced with the Unicode replacement character (U+FFFD)
 * 
 * Return value: a pointer to a newly allocated UTF-16 string.
 *               This value must be freed with delete[]
 **/
WCHAR *
hippo_utf8_to_utf16 (const char *str,
                     long        len) throw (std::bad_alloc)
{
  WCHAR *result;
  int n16;

  g_return_val_if_fail (str != NULL, NULL);

  n16 = utf8_to_utf16_count(str, len);
  result = new WCHAR[n16 + 1];
  utf8_to_utf16_output(str, len, n16, result);
  result[n16] = 0;

  return result;
}

#ifdef WIN32
/**
 * utf8_to_bstr:
 * @str: a UTF-8 encoded string
 * @len: the maximum length (number of characters) of @str to use. 
 *       If @len < 0, then the string is nul-terminated.
 *
 * Convert a string from UTF-8 to UTF-16. A 0 character will be
 * added to the result after the converted text. Invalid sequences
 * are replaced with the Unicode replacement character (U+FFFD)
 * 
 * Return value: A newly allocated BSTR. Free with SysFreeString()
 **/
BSTR
hippo_utf8_to_bstr (const char *str,
                    long        len) throw (std::bad_alloc)
{
  WCHAR *result;
  int n16;

  g_return_val_if_fail (str != NULL, NULL);

  n16 = utf8_to_utf16_count(str, len);
  result = SysAllocStringLen(NULL, n16);
  if (!result)
    throw std::bad_alloc();
  
  utf8_to_utf16_output(str, len, n16, result);
  
  return result;
}
#endif

#define CONTINUATION_CHAR                           \
 G_STMT_START {                                     \
  if ((*(guchar *)p & 0xc0) != 0x80) /* 10xxxxxx */ \
    goto error;                                     \
  val <<= 6;                                        \
  val |= (*(guchar *)p) & 0x3f;                     \
 } G_STMT_END

static const char *
fast_validate (const char *str)

{
  gunichar val = 0;
  gunichar min = 0;
  const char *p;

  for (p = str; *p; p++)
    {
      if (*(guchar *)p < 128)
        /* done */;
      else 
        {
          const char *last;
          
          last = p;
          if ((*(guchar *)p & 0xe0) == 0xc0) /* 110xxxxx */
            {
              if (G_UNLIKELY ((*(guchar *)p & 0x1e) == 0))
                goto error;
              p++;
              if (G_UNLIKELY ((*(guchar *)p & 0xc0) != 0x80)) /* 10xxxxxx */
                goto error;
            }
          else
            {
              if ((*(guchar *)p & 0xf0) == 0xe0) /* 1110xxxx */
                {
                  min = (1 << 11);
                  val = *(guchar *)p & 0x0f;
                  goto TWO_REMAINING;
                }
              else if ((*(guchar *)p & 0xf8) == 0xf0) /* 11110xxx */
                {
                  min = (1 << 16);
                  val = *(guchar *)p & 0x07;
                }
              else
                goto error;
              
              p++;
              CONTINUATION_CHAR;
            TWO_REMAINING:
              p++;
              CONTINUATION_CHAR;
              p++;
              CONTINUATION_CHAR;
              
              if (G_UNLIKELY (val < min))
                goto error;

              if (G_UNLIKELY (!UNICODE_VALID(val)))
                goto error;
            } 
          
          continue;
          
        error:
          return last;
        }
    }

  return p;
}

static const char *
fast_validate_len (const char *str,
                   intptr_t    max_len)

{
  gunichar val = 0;
  gunichar min = 0;
  const char *p;

  for (p = str; (max_len < 0 || (p - str) < max_len) && *p; p++)
    {
      if (*(guchar *)p < 128)
        /* done */;
      else 
        {
          const char *last;
          
          last = p;
          if ((*(guchar *)p & 0xe0) == 0xc0) /* 110xxxxx */
            {
              if (G_UNLIKELY (max_len >= 0 && max_len - (p - str) < 2))
                goto error;
              
              if (G_UNLIKELY ((*(guchar *)p & 0x1e) == 0))
                goto error;
              p++;
              if (G_UNLIKELY ((*(guchar *)p & 0xc0) != 0x80)) /* 10xxxxxx */
                goto error;
            }
          else
            {
              if ((*(guchar *)p & 0xf0) == 0xe0) /* 1110xxxx */
                {
                  if (G_UNLIKELY (max_len >= 0 && max_len - (p - str) < 3))
                    goto error;
                  
                  min = (1 << 11);
                  val = *(guchar *)p & 0x0f;
                  goto TWO_REMAINING;
                }
              else if ((*(guchar *)p & 0xf8) == 0xf0) /* 11110xxx */
                {
                  if (G_UNLIKELY (max_len >= 0 && max_len - (p - str) < 4))
                    goto error;
                  
                  min = (1 << 16);
                  val = *(guchar *)p & 0x07;
                }
              else
                goto error;
              
              p++;
              CONTINUATION_CHAR;
            TWO_REMAINING:
              p++;
              CONTINUATION_CHAR;
              p++;
              CONTINUATION_CHAR;
              
              if (G_UNLIKELY (val < min))
                goto error;
              if (G_UNLIKELY (!UNICODE_VALID(val)))
                goto error;
            } 
          
          continue;
          
        error:
          return last;
        }
    }

  return p;
}

/**
 * utf8_validate:
 * @str: a pointer to character data
 * @max_len: max bytes to validate, or -1 to go until NUL
 * @end: return location for end of valid data
 * 
 * Validates UTF-8 encoded text. @str is the text to validate;
 * if @str is nul-terminated, then @max_len can be -1, otherwise
 * @max_len should be the number of bytes to validate.
 * If @end is non-%NULL, then the end of the valid range
 * will be stored there (i.e. the start of the first invalid 
 * character if some bytes were invalid, or the end of the text 
 * being validated otherwise).
 *
 * Note that utf8_validate() returns %FALSE if @max_len is 
 * positive and NUL is met before @max_len bytes have been read.
 *
 * Returns %TRUE if all of @str was valid. Many GLib and GTK+
 * routines <emphasis>require</emphasis> valid UTF-8 as input;
 * so data read from a file or the network should be checked
 * with utf8_validate() before doing anything else with it.
 * 
 * Return value: %TRUE if the text was valid UTF-8
 **/
bool
hippo_utf8_validate (const char   *str,
                     ssize_t       max_len,    
                     const char  **end)

{
  const char *p;

  if (max_len < 0)
    p = fast_validate (str);
  else
    p = fast_validate_len (str, max_len);

  if (end)
    *end = p;

  if ((max_len >= 0 && p != str + max_len) ||
      (max_len < 0 && *p != '\0'))
    return false;
  else
    return true;
}

#ifdef BUILD_TEST

#include <string.h>
#include <stdio.h>

void test_round_trip(const char *utf8, const char *expected = 0)
{
  WCHAR *utf16 = hippo_utf8_to_utf16(utf8, -1);
  char *tripped = hippo_utf16_to_utf8(utf16, -1);

  bool valid = hippo_utf8_validate(utf8, -1, NULL);

  // UTF-8 should fail to validate iff. the caller provided the
  // the expected output (if it's valid, we know the expected
  // output is the input sequence)
  if (expected == 0)
    {
      if (!valid)
        fprintf(stderr, "Sequence '%s' unexpectedly did not validate\n", utf8);
      expected = utf8;
    }
  else
    {
      if (valid)
        fprintf(stderr, "Sequence '%s' unexpectedly validated\n", utf8);
    }
    
  if (strcmp(tripped, expected) != 0)
    fprintf(stderr, "Round trip failed for '%s', got '%s', expected '%s'\n", utf8, tripped, expected);

  delete[] utf16;
  delete[] tripped;
}

void test_to_utf8(const WCHAR *utf16, const char *expected)
{
  char *utf8 = hippo_utf16_to_utf8(utf16, -1);

  if (strcmp(utf8, expected) != 0)
    fprintf(stderr, "Conversion failed got '%s', expected '%s'\n", utf8, expected);
    
  delete[] utf8;
}

#define RC "\357\277\275"

int main()
{
  test_round_trip("A");
  test_round_trip("\304\200"); // U+0100 CAPITAL LETTER A WITH MACRON
  test_round_trip("\342\202\254"); // U+20AC Euro sign
  test_round_trip("\360\220\200\200"); // Beyond BMP
  test_round_trip("\360\220\200", RC); // Partial sequence at end
  test_round_trip("\220", RC); // invalid sequence
  test_round_trip("A\220A", "A" RC "A"); // invalid sequence in middle
  test_round_trip("\360\200\200\200", RC); // Overlong representation in UTF-8
  test_round_trip("\355\240\200", RC); // Surrogate
  test_round_trip("\360\277\277\277", RC); // Out of range for UTF-16

  static const WCHAR str1[] = { 0x0041, 0xd800, 0x0041, 0 } ;  // Unpaired high surrogate
  test_to_utf8(str1, "A" RC "A");
  static const WCHAR str2[] = { 0x0041, 0xd800, 0 } ;  // Unpaired high surrogate at end
  test_to_utf8(str2, "A" RC);
  static const WCHAR str3[] = { 0x0041, 0xdc00, 0x0041, 0 } ;  // Unpaired low surrogate
  test_to_utf8(str3, "A" RC "A");
    
}
#endif
