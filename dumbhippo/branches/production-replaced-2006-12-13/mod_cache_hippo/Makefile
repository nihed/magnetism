CFLAGS=-fPIC -g -Wall -O2 -I/usr/include/httpd -I/usr/include/apr-0

HEADERS =		\
	mod_cache.h

OBJECTS = 		\
	mod_cache.o	\
	cache_storage.o	\
	cache_util.o	\
	hippo_cache.o

DISK_CACHE_OBJECTS = \
	mod_disk_cache.o

TEST_OBJECTS = 		\
	hippo_cache_test.o

all: mod_cache_hippo.so mod_disk_cache_hippo.so hippo_cache_test

mod_cache_hippo.so: $(OBJECTS)
	gcc -shared -o $@ $(OBJECTS) -lpthread

mod_disk_cache_hippo.so: $(DISK_CACHE_OBJECTS)
	gcc -shared -o $@ $(DISK_CACHE_OBJECTS) -lpthread

hippo_cache_test.o: hippo_cache.c
	gcc -c $(CFLAGS) $(CPPFLAGS) -DTEST -o $@ $<

hippo_cache_test: $(TEST_OBJECTS)
	gcc -o $@ $(TEST_OBJECTS) -lpthread -lapr-0

clean:
	rm -rf $(OBJECTS) $(TEST_OBJECTS) mod_cache_hippo.so hippo_cache_test

$(OBJECTS) $(TEST_OBJECTS): $(HEADERS)
