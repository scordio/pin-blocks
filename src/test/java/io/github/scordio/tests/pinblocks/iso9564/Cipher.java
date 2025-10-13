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
package io.github.scordio.tests.pinblocks.iso9564;

import io.github.scordio.pinblocks.iso9564.Decryptor;
import io.github.scordio.pinblocks.iso9564.Encryptor;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

enum Cipher implements Encryptor, Decryptor {

	AES_ECB("AES/ECB/NoPadding");

	private static final SecretKey key = new SecretKeySpec(new byte[32], "AES");

	private final javax.crypto.Cipher cipher;

	Cipher(String transformation) {
		try {
			cipher = javax.crypto.Cipher.getInstance(transformation);
		}
		catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] encrypt(byte[] input) {
		try {
			cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal(input);
		}
		catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] decrypt(byte[] input) {
		try {
			cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(input);
		}
		catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

}
