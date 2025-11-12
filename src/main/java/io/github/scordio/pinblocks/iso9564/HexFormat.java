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

import java.util.Objects;

class HexFormat {

	static byte[] parseHex(CharSequence source) {
		int length = source.length();
		if (length % 2 != 0) {
			throw new IllegalArgumentException("'source' must have even length, but was: " + source.length());
		}

		byte[] bytes = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			int hi = getDigit(source.charAt(i));
			int lo = getDigit(source.charAt(i + 1));
			bytes[i / 2] = (byte) ((hi << 4) | lo);
		}

		return bytes;
	}

	private static int getDigit(char character) {
		int digit = Character.digit(character, 16);
		if (digit == -1) {
			throw new IllegalArgumentException("Invalid hex character: " + character);
		}
		return digit;
	}

	static char[] formatHex(byte[] source) {
		Objects.requireNonNull(source, "'null' is not supported");

		char[] result = new char[source.length * 2];
		for (int i = 0; i < source.length; i++) {
			byte b = source[i];
			int hi = (b >> 4) & 0x0F;
			int lo = b & 0x0F;
			result[i * 2] = Character.toUpperCase(Character.forDigit(hi, 16));
			result[i * 2 + 1] = Character.toUpperCase(Character.forDigit(lo, 16));
		}
		return result;
	}

}
