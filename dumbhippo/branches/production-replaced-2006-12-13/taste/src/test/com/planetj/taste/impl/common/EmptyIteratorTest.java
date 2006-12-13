/*
 * Copyright 2005 Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.planetj.taste.impl.common;

import com.planetj.taste.impl.TasteTestCase;

import java.util.NoSuchElementException;

/**
 * @author Sean Owen
 */
public final class EmptyIteratorTest extends TasteTestCase {

	public void testIterator() {
		final EmptyIterator<Object> mock = new EmptyIterator<Object>();
		assertFalse(mock.hasNext());
		try {
			mock.next();
			fail("Should have thrown NoSuchElementException");
		} catch (NoSuchElementException nsee) {
			// good
		}
		try {
			mock.remove();
			fail("Should have thrown UnsupportedOperationException");
		} catch (UnsupportedOperationException uoe) {
			// good
		}
	}

	public void testIterable() {
		final EmptyIterable<Object> mock = new EmptyIterable<Object>();
		assertNotNull(mock.iterator());
	}

}
