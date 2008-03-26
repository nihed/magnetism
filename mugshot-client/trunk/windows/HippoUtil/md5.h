#pragma once

#ifdef BUILDING_HIPPO_UTIL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif

#ifdef __alpha
typedef unsigned int uint32;
#else
typedef unsigned long uint32;
#endif

struct MD5Context {
        uint32 buf[4];
        uint32 bits[2];
        unsigned char in[64];
};

void DLLEXPORT MD5Init(struct MD5Context *ctx);
void DLLEXPORT MD5Update(struct MD5Context *ctx, unsigned char *buf, unsigned len);
void DLLEXPORT MD5Final(unsigned char digest[16], struct MD5Context *ctx);
void DLLEXPORT MD5Transform(uint32 buf[4], uint32 in[16]);

/*
 * This is needed to make RSAREF happy on some MS-DOS compilers.
 */
typedef struct MD5Context MD5_CTX;
