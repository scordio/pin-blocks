/*
 * Copyright © 2025 the original author or authors.
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
package io.github.scordio.pinblocks.iso9564;

/**
 * Generates random bytes as part of PIN block encoding.
 * <p>
 * This abstraction is context-agnostic: it defines no specific source of randomness. The
 * required length is determined by the calling context via the size of the supplied
 * array.
 *
 * @see Format4.Encoder#withRandomGenerator(RandomGenerator)
 */
@FunctionalInterface
public interface RandomGenerator {

	/**
	 * Fills the given array with random bytes.
	 * <p>
	 * Implementations must provide cryptographically secure random bytes suitable for
	 * sensitive operations, and must fill the entire array.
	 * @param bytes the array to fill with random bytes; never {@code null}
	 */
	void nextBytes(byte[] bytes);

}
