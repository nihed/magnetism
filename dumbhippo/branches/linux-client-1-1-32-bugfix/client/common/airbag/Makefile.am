## Process this file with automake to produce Makefile.in

# Copyright (c) 2006, Google Inc.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#
#     * Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above
# copyright notice, this list of conditions and the following disclaimer
# in the documentation and/or other materials provided with the
# distribution.
#     * Neither the name of Google Inc. nor the names of its
# contributors may be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


# This allows #includes to be relative to src/
AM_CPPFLAGS = -I$(top_srcdir)/src


## Documentation
docdir = $(prefix)/share/doc/$(PACKAGE)-$(VERSION)

dist_doc_DATA = \
	AUTHORS \
	COPYING \
	ChangeLog \
	INSTALL \
	NEWS \
	README


## Libraries
lib_LTLIBRARIES = src/libairbag.la

src_libairbag_la_SOURCES = \
	src/google_airbag/common/airbag_types.h \
	src/google_airbag/common/minidump_format.h \
	src/google_airbag/processor/basic_source_line_resolver.h \
	src/google_airbag/processor/call_stack.h \
	src/google_airbag/processor/code_module.h \
	src/google_airbag/processor/code_modules.h \
	src/google_airbag/processor/memory_region.h \
	src/google_airbag/processor/minidump.h \
	src/google_airbag/processor/minidump_processor.h \
	src/google_airbag/processor/process_state.h \
	src/google_airbag/processor/stack_frame.h \
	src/google_airbag/processor/stack_frame_cpu.h \
	src/google_airbag/processor/stackwalker.h \
	src/google_airbag/processor/symbol_supplier.h \
	src/google_airbag/processor/system_info.h \
	src/processor/address_map.h \
	src/processor/address_map-inl.h \
	src/processor/basic_code_module.h \
	src/processor/basic_code_modules.cc \
	src/processor/basic_code_modules.h \
	src/processor/basic_source_line_resolver.cc \
	src/processor/call_stack.cc \
	src/processor/contained_range_map.h \
	src/processor/contained_range_map-inl.h \
	src/processor/linked_ptr.h \
	src/processor/minidump.cc \
	src/processor/minidump_processor.cc \
	src/processor/pathname_stripper.cc \
	src/processor/pathname_stripper.h \
	src/processor/postfix_evaluator.h \
	src/processor/postfix_evaluator-inl.h \
	src/processor/process_state.cc \
	src/processor/range_map.h \
	src/processor/range_map-inl.h \
	src/processor/scoped_ptr.h \
	src/processor/simple_symbol_supplier.cc \
	src/processor/simple_symbol_supplier.h \
	src/processor/stack_frame_info.h \
	src/processor/stackwalker.cc \
	src/processor/stackwalker_ppc.cc \
	src/processor/stackwalker_ppc.h \
	src/processor/stackwalker_x86.cc \
	src/processor/stackwalker_x86.h


## Programs
bin_PROGRAMS = \
	src/processor/minidump_dump \
	src/processor/minidump_stackwalk


## Tests
check_PROGRAMS = \
	src/processor/address_map_unittest \
	src/processor/basic_source_line_resolver_unittest \
	src/processor/contained_range_map_unittest \
	src/processor/minidump_processor_unittest \
	src/processor/pathname_stripper_unittest \
	src/processor/postfix_evaluator_unittest \
	src/processor/range_map_unittest

if SELFTEST
check_PROGRAMS += \
	src/processor/stackwalker_selftest
endif SELFTEST

check_SCRIPTS = \
	src/processor/minidump_dump_test \
	src/processor/minidump_stackwalk_test

TESTS = $(check_PROGRAMS) $(check_SCRIPTS)
TESTS_ENVIRONMENT =

src_processor_address_map_unittest_SOURCES = \
	src/processor/address_map_unittest.cc

src_processor_basic_source_line_resolver_unittest_SOURCES = \
	src/processor/basic_source_line_resolver_unittest.cc
src_processor_basic_source_line_resolver_unittest_LDADD = \
	src/processor/basic_source_line_resolver.lo

src_processor_contained_range_map_unittest_SOURCES = \
	src/processor/contained_range_map_unittest.cc

src_processor_minidump_processor_unittest_SOURCES = \
	src/processor/minidump_processor_unittest.cc
src_processor_minidump_processor_unittest_LDADD = \
	src/processor/basic_code_modules.lo \
	src/processor/basic_source_line_resolver.lo \
	src/processor/call_stack.lo \
	src/processor/minidump_processor.lo \
	src/processor/minidump.lo \
	src/processor/process_state.lo \
	src/processor/stackwalker.lo \
	src/processor/stackwalker_ppc.lo \
	src/processor/stackwalker_x86.lo

src_processor_pathname_stripper_unittest_SOURCES = \
	src/processor/pathname_stripper_unittest.cc
src_processor_pathname_stripper_unittest_LDADD = \
	src/processor/pathname_stripper.lo

src_processor_postfix_evaluator_unittest_SOURCES = \
	src/processor/postfix_evaluator_unittest.cc

src_processor_range_map_unittest_SOURCES = \
	src/processor/range_map_unittest.cc

src_processor_stackwalker_selftest_SOURCES = \
	src/processor/stackwalker_selftest.cc
src_processor_stackwalker_selftest_LDADD = \
	src/processor/basic_code_modules.lo \
	src/processor/basic_source_line_resolver.lo \
	src/processor/call_stack.lo \
	src/processor/minidump.lo \
	src/processor/stackwalker.lo \
	src/processor/stackwalker_ppc.lo \
	src/processor/stackwalker_x86.lo

## Non-installables
noinst_PROGRAMS =
noinst_SCRIPTS = $(check_SCRIPTS)

src_processor_minidump_dump_SOURCES = \
	src/processor/minidump_dump.cc
src_processor_minidump_dump_LDADD = \
	src/processor/basic_code_modules.lo \
	src/processor/minidump.lo

src_processor_minidump_stackwalk_SOURCES = \
	src/processor/minidump_stackwalk.cc
src_processor_minidump_stackwalk_LDADD = \
	src/processor/basic_code_modules.lo \
	src/processor/basic_source_line_resolver.lo \
	src/processor/call_stack.lo \
	src/processor/minidump.lo \
	src/processor/minidump_processor.lo \
	src/processor/pathname_stripper.lo \
	src/processor/process_state.lo \
	src/processor/simple_symbol_supplier.lo \
	src/processor/stackwalker.lo \
	src/processor/stackwalker_ppc.lo \
	src/processor/stackwalker_x86.lo


## Additional files to be included in a source distribution
##
## find src/client src/common src/processor/testdata src/tools \
##     -type f \! -wholename '*/.svn*' -print | sort
EXTRA_DIST = \
	$(SCRIPTS) \
	src/client/minidump_file_writer.cc \
	src/client/minidump_file_writer.h \
	src/client/minidump_file_writer-inl.h \
	src/client/windows/airbag_client.sln \
	src/client/windows/handler/exception_handler.cc \
	src/client/windows/handler/exception_handler.h \
	src/client/windows/handler/exception_handler.vcproj \
	src/client/windows/sender/crash_report_sender.cc \
	src/client/windows/sender/crash_report_sender.h \
	src/client/windows/sender/crash_report_sender.vcproj \
	src/common/windows/guid_string.cc \
	src/common/windows/guid_string.h \
	src/common/windows/http_upload.cc \
	src/common/windows/http_upload.h \
	src/common/windows/pdb_source_line_writer.cc \
	src/common/windows/pdb_source_line_writer.h \
	src/common/windows/string_utils-inl.h \
	src/processor/testdata/minidump2.dmp \
	src/processor/testdata/minidump2.dump.out \
	src/processor/testdata/minidump2.stackwalk.out \
	src/processor/testdata/module1.out \
	src/processor/testdata/module2.out \
	src/processor/testdata/module3_bad.out \
	src/processor/testdata/symbols/kernel32.pdb/BCE8785C57B44245A669896B6A19B9542/kernel32.sym \
	src/processor/testdata/symbols/test_app.pdb/8DDB7E9A365748938D6EB08B1DCA31AA1/test_app.sym \
	src/processor/testdata/test_app.cc \
	src/tools/windows/dump_syms/dump_syms.cc \
	src/tools/windows/dump_syms/dump_syms.vcproj \
	src/tools/windows/dump_syms/run_regtest.sh \
	src/tools/windows/dump_syms/testdata/dump_syms_regtest.out \
	src/tools/windows/dump_syms/testdata/dump_syms_regtest.pdb \
	src/tools/windows/symupload/symupload.cc \
	src/tools/windows/symupload/symupload.vcproj


## Additional rules
libtool: $(LIBTOOL_DEPS)
	$(SHELL) ./config.status --recheck
