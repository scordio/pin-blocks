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

import io.github.scordio.junit.converters.Hex;
import io.github.scordio.pinblocks.iso9564.Format4;
import io.github.scordio.pinblocks.iso9564.Format4.Decoder;
import io.github.scordio.pinblocks.iso9564.Format4.Encoder;
import io.github.scordio.pinblocks.iso9564.RandomGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static io.github.scordio.tests.pinblocks.iso9564.Cipher.AES_ECB;
import static org.assertj.core.api.BDDAssertions.catchException;
import static org.assertj.core.api.BDDAssertions.then;

class Format4Tests {

	@ParameterizedTest
	@CsvSource(textBlock = """
			123456, 000000
			123456, 123456789012
			123456, 1234567890123
			123456, 1234567890123456789
			""")
	void encode(CharSequence pin, String pan) {
		// Given
		Encoder underTest = Format4.encoder().withEncryptor(AES_ECB);

		// When
		byte[] pinBlock = underTest.encode(pin, pan);

		// Then
		then(pinBlock).hasSize(16);

		Decoder decoder = Format4.decoder().withDecryptor(AES_ECB);
		then(new String(decoder.decode(pinBlock, pan)).contentEquals(pin)).isTrue();
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			123456, 000000,              5235DF2C339E3994438AF0F4C38EC09E
			123456, 123456789012,        B80C1E4CFC6C801F08C4540521AA14E4
			123456, 1234567890123,       8C973F18F40FBF649F69944353B48BA5
			123456, 1234567890123456789, 24F7109DD6C824CCCFB0B1D004313361
			""")
	void encode_with_custom_random_generator(CharSequence pin, String pan, @Hex byte[] expected) {
		// Given
		RandomGenerator alwaysZeros = bytes -> Arrays.fill(bytes, (byte) 0x00);
		Encoder underTest = Format4.encoder().withEncryptor(AES_ECB).withRandomGenerator(alwaysZeros);

		// When
		byte[] pinBlock = underTest.encode(pin, pan);

		// Then
		then(pinBlock).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			32D4E29C54B2075B86DF599A776C3FA5, 000000,              123456
			5235DF2C339E3994438AF0F4C38EC09E, 000000,              123456
			B80C1E4CFC6C801F08C4540521AA14E4, 123456789012,        123456
			8C973F18F40FBF649F69944353B48BA5, 1234567890123,       123456
			24F7109DD6C824CCCFB0B1D004313361, 1234567890123456789, 123456
			""")
	void decode(@Hex byte[] pinBlock, String pan, String expected) {
		// Given
		Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

		// When
		char[] pin = underTest.decode(pinBlock, pan);

		// Then
		then(new String(pin)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			123
			123X
			""")
	void encode_fails_with_invalid_pin(CharSequence pin) {
		// Given
		Encoder underTest = Format4.encoder().withEncryptor(AES_ECB);

		// When
		Exception exception = catchException(() -> underTest.encode(pin, "000000"));

		// Then
		then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN");
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			12A4
			12345678901234567890
			""")
	void encode_fails_with_invalid_pan(String pan) {
		// Given
		Encoder underTest = Format4.encoder().withEncryptor(AES_ECB);

		// When
		Exception exception = catchException(() -> underTest.encode("1234", pan));

		// Then
		then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PAN");
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			# PIN: 34AAAAAA
			60F96E147DC14762A711A020086A3042, 000000
			""")
	void decode_fails_with_non_decimal_pin_digits(@Hex byte[] pinBlock, String pan) {
		// Given
		Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

		// When
		Exception exception = catchException(() -> underTest.decode(pinBlock, pan));

		// Then
		then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN digit");
	}

}
