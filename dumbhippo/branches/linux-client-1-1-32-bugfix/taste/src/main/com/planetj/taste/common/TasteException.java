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

package com.planetj.taste.common;

/**
 * <p>An exception thrown when an error occurs inside the Taste engine.</p>
 *
 * @author Sean Owen
 */
public final class TasteException extends Exception {

	private static final long serialVersionUID = -4642423577377913441L;

	public TasteException() {
		super();
	}

	public TasteException(final String message) {
		super(message);
	}

	public TasteException(final Throwable cause) {
		super(cause);
	}

	public TasteException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
