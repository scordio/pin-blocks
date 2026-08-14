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
 * Decrypts a block of data as part of PIN block decoding.
 * <p>
 * This abstraction is context-agnostic: it defines no specific cipher or block size. The
 * expected input/output length and cryptographic algorithm are determined by the calling
 * context.
 *
 * @see Format4.Decoder.Builder#withDecryptor(Decryptor)
 */
@FunctionalInterface
public interface Decryptor {

	/**
	 * Decrypts the given bytes.
	 * <p>
	 * Implementations should not make any assumptions about how the returned byte array
	 * will be used by callers, including whether it may be retained, modified, or zeroed
	 * after return.
	 * @param input the bytes to decrypt; never {@code null}
	 * @return the decrypted bytes; never {@code null}
	 */
	byte[] decrypt(byte[] input);

}
