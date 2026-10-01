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
 * Encrypts a block of data as part of PIN block encoding.
 * <p>
 * This abstraction is context-agnostic: it defines no specific cipher or block size. The
 * expected input/output length and cryptographic algorithm are determined by the calling
 * context.
 *
 * @see Format4.Encoder.Builder#withEncryptor(Encryptor)
 */
@FunctionalInterface
public interface Encryptor {

	/**
	 * Encrypts the given bytes.
	 * <p>
	 * Implementations must return a new, independent array, leaving the input unmodified
	 * and never returning the same instance, and should not make any assumptions about
	 * how the returned array will be used afterward, including whether it may be
	 * retained, modified, or zeroed.
	 * @param input the bytes to encrypt; never {@code null}
	 * @return the encrypted bytes; never {@code null}
	 */
	byte[] encrypt(byte[] input);

}
