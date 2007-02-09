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

import com.planetj.taste.common.TasteException;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>An efficient Map-like class which caches values for keys. Values are held with {@link SoftReference}s,
 * so that they can be reclaimed in low-memory situations. Values are not "put" into a {@link SoftCache};
 * instead the caller supplies the instance with an implementation of {@link Retriever} which can load the
 * value for a given key.</p>
 *
 * @author Sean Owen
 */
public final class SoftCache<K, V> {

	private final Map<K, SoftReference<V>> cache;
	private final Retriever<? super K, ? extends V> retriever;

	/**
	 * <p>Creates a new cache based on the given {@link Retriever}.</p>
	 *
	 * @param retriever
	 */
	public SoftCache(final Retriever<? super K, ? extends V> retriever) {
		if (retriever == null) {
			throw new IllegalArgumentException();
		}
		cache = new ConcurrentHashMap<K, SoftReference<V>>(1009, 0.5f, 4);
		this.retriever = retriever;
	}

	/**
	 * <p>Returns cached value for a key. If it does not exist, it is loaded using a {@link Retriever}.</p>
	 *
	 * @param key
	 * @return value for that key
	 * @throws TasteException
	 */
	@NotNull
	public V get(final K key) throws TasteException {
		final SoftReference<V> reference = cache.get(key);
		if (reference == null) {
			return getAndCacheValue(key);
		}
		final V value = reference.get();
		if (value == null) {
			return getAndCacheValue(key);
		}
		return value;
	}

	/**
	 * <p>Uncaches any existing value for a given key.</p>
	 *
	 * @param key
	 */
	public void remove(final K key) {
		cache.remove(key);
	}

	/**
	 * <p>Clears the cache.</p>
	 */
	public void clear() {
		cache.clear();
	}

	@NotNull
	private V getAndCacheValue(final K key) throws TasteException {
		final V value = retriever.getValue(key);
		if (value == null) {
			throw new IllegalStateException();
		}
		cache.put(key, new SoftReference<V>(value));
		return value;
	}

	/**
	 * <p>Implementations can retrieve a value for a given key.</p>
	 */
	public static interface Retriever<KK, VV> {
		VV getValue(KK key) throws TasteException;
	}

}
