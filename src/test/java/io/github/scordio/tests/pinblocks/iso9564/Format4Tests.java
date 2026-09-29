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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static io.github.scordio.tests.pinblocks.iso9564.Cipher.AES_ECB;
import static org.assertj.core.api.BDDAssertions.catchException;
import static org.assertj.core.api.BDDAssertions.then;

class Format4Tests {

	@Nested
	class Encode {

		private final Encoder underTest = Format4.encoder().withEncryptor(AES_ECB);

		@ParameterizedTest
		@ValueSource(strings = { "1234", "123456789012" })
		void should_succeed_with_valid_pin(CharSequence pin) {
			// Given
			String pan = "";

			// When
			byte[] pinBlock = underTest.encode(pin, pan);

			// Then
			then(pinBlock).hasSize(16);

			Decoder decoder = Format4.decoder().withDecryptor(AES_ECB);
			then(new String(decoder.decode(pinBlock, pan))).isEqualTo(pin);
		}

		@ParameterizedTest
		@ValueSource(strings = { "", "1", "12345678901", "123456789012", "1234567890123456789" })
		void should_succeed_with_valid_pan(String pan) {
			// Given
			CharSequence pin = "1234";

			// When
			byte[] pinBlock = underTest.encode(pin, pan);

			// Then
			then(pinBlock).hasSize(16);

			Decoder decoder = Format4.decoder().withDecryptor(AES_ECB);
			then(new String(decoder.decode(pinBlock, pan))).isEqualTo(pin);
		}

		@ParameterizedTest
		@CsvSource(textBlock = """
				123456, 000000,              5235DF2C339E3994438AF0F4C38EC09E
				123456, 123456789012,        B80C1E4CFC6C801F08C4540521AA14E4
				123456, 1234567890123,       8C973F18F40FBF649F69944353B48BA5
				123456, 1234567890123456789, 24F7109DD6C824CCCFB0B1D004313361
				""")
		void should_succeed_with_custom_random_generator(CharSequence pin, String pan, @Hex byte[] expected) {
			// Given
			RandomGenerator alwaysZeros = bytes -> Arrays.fill(bytes, (byte) 0x00);
			Encoder underTest = this.underTest.withRandomGenerator(alwaysZeros);

			// When
			byte[] pinBlock = underTest.encode(pin, pan);

			// Then
			then(pinBlock).isEqualTo(expected);
		}

		@ParameterizedTest
		@ValueSource(strings = { "123", "123A", "1234567890123" })
		void should_fail_if_pin_is_invalid(CharSequence pin) {
			// Given
			String pan = "";

			// When
			Exception exception = catchException(() -> underTest.encode(pin, pan));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN");
		}

		@ParameterizedTest
		@ValueSource(strings = { "A", "12345678901234567890" })
		void should_fail_if_pan_is_invalid(String pan) {
			// Given
			String pin = "1234";

			// When
			Exception exception = catchException(() -> underTest.encode(pin, pan));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PAN");
		}

	}

	@Nested
	class Decode {

		@ParameterizedTest
		@CsvSource(textBlock = """
				32D4E29C54B2075B86DF599A776C3FA5, 000000,              123456
				5235DF2C339E3994438AF0F4C38EC09E, 000000,              123456
				B80C1E4CFC6C801F08C4540521AA14E4, 123456789012,        123456
				8C973F18F40FBF649F69944353B48BA5, 1234567890123,       123456
				24F7109DD6C824CCCFB0B1D004313361, 1234567890123456789, 123456
				""")
		void should_succeed(@Hex byte[] pinBlock, String pan, String expected) {
			// Given
			Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

			// When
			char[] pin = underTest.decode(pinBlock, pan);

			// Then
			then(new String(pin)).isEqualTo(expected);
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"F4F2E1D19FCCDC7CAF41A520D18A96F3" // PIN control field: 3
		})
		void should_fail_if_pin_control_field_is_invalid(@Hex byte[] pinBlock) {
			// Given
			String pan = "000000";
			Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, pan));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN control field");
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"ABE837E8DC3AA1E678662CD3F33B6D6E", // PIN length: 3
				"4F30822DDC9AACDB95DB1D480FCAB06D", // PIN length: 13
		})
		void should_fail_if_pin_length_is_invalid(@Hex byte[] pinBlock) {
			// Given
			String pan = "000000";
			Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, pan));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN length");
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"193B29D12E2039FAB36DD78F4626A97F" // Fill digits: AAAAAAAB
		})
		void should_fail_if_fill_digits_are_invalid(@Hex byte[] pinBlock) {
			// Given
			String pan = "000000";
			Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, pan));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid fill digits");
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"2590B13382E284FAEA90D002269E01F4" // PIN: 12345A
		})
		void should_fail_if_pin_contains_non_decimal_digits(@Hex byte[] pinBlock) {
			// Given
			String pan = "000000";
			Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, pan));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN digit");
		}

	}

}
