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
import io.github.scordio.pinblocks.iso9564.Decryptor;
import io.github.scordio.pinblocks.iso9564.Encryptor;
import io.github.scordio.pinblocks.iso9564.Format4;
import io.github.scordio.pinblocks.iso9564.Format4.Decoder;
import io.github.scordio.pinblocks.iso9564.Format4.Encoder;
import io.github.scordio.pinblocks.iso9564.RandomGenerator;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.scordio.tests.pinblocks.iso9564.Cipher.AES_ECB;
import static org.assertj.core.api.BDDAssertions.catchException;
import static org.assertj.core.api.BDDAssertions.then;

class Format4Tests {

	@Nested
	class EncoderTests {

		private final Encoder underTest = Format4.encoder().withEncryptor(AES_ECB);

		@Test
		void builder_should_fail_if_encryptor_is_null() {
			// When
			Exception exception = catchException(() -> Format4.encoder().withEncryptor(null));

			// Then
			then(exception).isInstanceOf(NullPointerException.class).hasMessage("'encryptor' must not be null");
		}

		@Test
		void builder_should_fail_if_generator_is_null() {
			// When
			Exception exception = catchException(() -> underTest.withRandomGenerator(null));

			// Then
			then(exception).isInstanceOf(NullPointerException.class).hasMessage("'generator' must not be null");
		}

		@ParameterizedTest
		@ValueSource(strings = { "1234", "123456789012" })
		void encode_should_succeed_if_pin_is_valid(CharSequence pin) {
			// When
			byte[] pinBlock = underTest.encode(pin, "");

			// Then
			then(pinBlock).hasSize(16);

			Decoder decoder = Format4.decoder().withDecryptor(AES_ECB);
			then(new String(decoder.decode(pinBlock, ""))).isEqualTo(pin);
		}

		@ParameterizedTest
		@ValueSource(strings = { "", "0", "1", "12345678901", "123456789012", "1234567890123456789" })
		void encode_should_succeed_if_pan_is_valid(String pan) {
			// Given
			CharSequence pin = "1234";

			// When
			byte[] pinBlock = underTest.encode(pin, pan);

			// Then
			then(pinBlock).hasSize(16);

			Decoder decoder = Format4.decoder().withDecryptor(AES_ECB);
			then(new String(decoder.decode(pinBlock, pan))).isEqualTo(pin);
		}

		@Test
		void encode_should_succeed_with_custom_random_generator() {
			// Given
			Encoder underTest = Format4.encoder()
				.withEncryptor(AES_ECB)
				.withRandomGenerator(new PredictableRandomGenerator());

			// When
			byte[] pinBlock = underTest.encode("1234", "");

			// Then
			then(pinBlock).asHexString().isEqualTo("1DE732FDA3B8F103737B4915848F9064");
		}

		@NullMarked
		private static class PredictableRandomGenerator implements RandomGenerator {

			private final Random random = new Random(42);

			@Override
			public void nextBytes(byte[] bytes) {
				random.nextBytes(bytes);
			}

		}

		@Test
		void encode_should_fail_if_pin_is_null() {
			// When
			Exception exception = catchException(() -> underTest.encode(null, ""));

			// Then
			then(exception).isInstanceOf(NullPointerException.class).hasMessage("'pin' must not be null");
		}

		@ParameterizedTest
		@ValueSource(strings = { "123", "123A", "1234567890123" })
		void encode_should_fail_if_pin_is_invalid(CharSequence pin) {
			// When
			Exception exception = catchException(() -> underTest.encode(pin, ""));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN");
		}

		@Test
		void encode_should_fail_if_pan_is_null() {
			// When
			Exception exception = catchException(() -> underTest.encode("1234", null));

			// Then
			then(exception).isInstanceOf(NullPointerException.class).hasMessage("'pan' must not be null");
		}

		@ParameterizedTest
		@ValueSource(strings = { "A", "12345678901234567890" })
		void encode_should_fail_if_pan_is_invalid(String pan) {
			// When
			Exception exception = catchException(() -> underTest.encode("1234", pan));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PAN");
		}

		@ParameterizedTest
		@ValueSource(ints = { 15, 17 })
		void encode_should_fail_if_encryptor_does_not_return_expected_block_length(int length) {
			// Given
			Encoder underTest = Format4.encoder().withEncryptor(_ -> new byte[length]);

			// When
			Exception exception = catchException(() -> underTest.encode("1234", ""));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid block length");
		}

		@ParameterizedTest
		@ValueSource(ints = { 15, 17 })
		void encode_should_fail_if_encryptor_does_not_return_expected_block_length_on_second_call(int length) {
			// Given
			AtomicInteger counter = new AtomicInteger();
			Encryptor encryptor = _ -> counter.incrementAndGet() % 2 != 0 ? new byte[16] : new byte[length];
			Encoder underTest = Format4.encoder().withEncryptor(encryptor);

			// When
			Exception exception = catchException(() -> underTest.encode("1234", ""));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid block length");
		}

	}

	@Nested
	class DecoderTests {

		private final Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

		@Test
		void builder_should_fail_if_decryptor_is_null() {
			// When
			Exception exception = catchException(() -> Format4.decoder().withDecryptor(null));

			// Then
			then(exception).isInstanceOf(NullPointerException.class).hasMessage("'decryptor' must not be null");
		}

		@ParameterizedTest
		@CsvSource("""
				F6E977BE4AC0DFE037AD73FC73477D35, 1234
				D477EFB63163F2689CBF9B0C8E0ED70D, 123456789012
				""")
		void decode_should_succeed_if_pin_block_is_valid(@Hex byte[] pinBlock, String expected) {
			// When
			char[] pin = underTest.decode(pinBlock, "");

			// Then
			then(new String(pin)).isEqualTo(expected);
		}

		@ParameterizedTest
		@CsvSource("""
				F6E977BE4AC0DFE037AD73FC73477D35, ''
				F6E977BE4AC0DFE037AD73FC73477D35, 0
				8724852232071D97BE2818004AFAE57A, 1
				36F0B9A5457D8680278830E46E30C560, 12345678901
				6F24663D2B1A2E8744D7AB340E8209FB, 123456789012
				0BC7A2FCA550804791763CB8B080D2C7, 1234567890123456789
				""")
		void decode_should_succeed_if_pan_is_valid(@Hex byte[] pinBlock, String pan) {
			// When
			char[] pin = underTest.decode(pinBlock, pan);

			// Then
			then(new String(pin)).isEqualTo("1234");
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"F6E977BE4AC0DFE037AD73FC73477D35", // PAN: ""
				"F6E977BE4AC0DFE037AD73FC73477D35", // PAN: 0
				"8724852232071D97BE2818004AFAE57A", // PAN: 1
				"36F0B9A5457D8680278830E46E30C560", // PAN: 12345678901
				"6F24663D2B1A2E8744D7AB340E8209FB", // PAN: 123456789012
				"0BC7A2FCA550804791763CB8B080D2C7", // PAN: 1234567890123456789
		})
		void decode_should_fail_if_pan_is_wrong(@Hex byte[] pinBlock) {
			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, "123456"));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void decode_should_fail_if_pin_block_is_null() {
			// When
			Exception exception = catchException(() -> underTest.decode(null, ""));

			// Then
			then(exception).isInstanceOf(NullPointerException.class).hasMessage("'pinBlock' must not be null");
		}

		@ParameterizedTest
		@ValueSource(ints = { 15, 17 })
		void decode_should_fail_if_pin_block_length_is_invalid(int length) {
			// When
			Exception exception = catchException(() -> underTest.decode(new byte[length], ""));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid block length");
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"F4F2E1D19FCCDC7CAF41A520D18A96F3" // PIN control field: 3
		})
		void decode_should_fail_if_pin_control_field_is_invalid(@Hex byte[] pinBlock) {
			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, "000000"));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN control field");
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"ABE837E8DC3AA1E678662CD3F33B6D6E", // PIN length: 3
				"4F30822DDC9AACDB95DB1D480FCAB06D", // PIN length: 13
		})
		void decode_should_fail_if_pin_length_is_invalid(@Hex byte[] pinBlock) {
			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, "000000"));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN length");
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"193B29D12E2039FAB36DD78F4626A97F" // Fill digits: AAAAAAAB
		})
		void decode_should_fail_if_fill_digits_are_invalid(@Hex byte[] pinBlock) {
			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, "000000"));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid fill digits");
		}

		@ParameterizedTest
		@ValueSource(strings = { //
				"2590B13382E284FAEA90D002269E01F4" // PIN: 12345A
		})
		void decode_should_fail_if_pin_has_non_decimal_digits(@Hex byte[] pinBlock) {
			// When
			Exception exception = catchException(() -> underTest.decode(pinBlock, "000000"));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PIN digit");
		}

		@Test
		void decode_should_fail_if_pan_is_null() {
			// When
			Exception exception = catchException(() -> underTest.decode(new byte[16], null));

			// Then
			then(exception).isInstanceOf(NullPointerException.class).hasMessage("'pan' must not be null");
		}

		@ParameterizedTest
		@ValueSource(strings = { "A", "12345678901234567890" })
		void decode_should_fail_if_pan_is_invalid(String pan) {
			// When
			Exception exception = catchException(() -> underTest.decode(new byte[16], pan));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid PAN");
		}

		@ParameterizedTest
		@ValueSource(ints = { 15, 17 })
		void decode_should_fail_if_decryptor_does_not_return_expected_block_length(int length) {
			// Given
			Decoder underTest = Format4.decoder().withDecryptor(_ -> new byte[length]);

			// When
			Exception exception = catchException(() -> underTest.decode(new byte[16], ""));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid block length");
		}

		@ParameterizedTest
		@ValueSource(ints = { 15, 17 })
		void decode_should_fail_if_decryptor_does_not_return_expected_block_length_on_second_call(int length) {
			// Given
			AtomicInteger counter = new AtomicInteger();
			Decryptor decryptor = _ -> counter.incrementAndGet() % 2 != 0 ? new byte[16] : new byte[length];
			Decoder underTest = Format4.decoder().withDecryptor(decryptor);

			// When
			Exception exception = catchException(() -> underTest.decode(new byte[16], ""));

			// Then
			then(exception).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid block length");
		}

	}

}
