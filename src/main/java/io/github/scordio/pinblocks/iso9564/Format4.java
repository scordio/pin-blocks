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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Format4 {

	private Format4() {
	}

	public static Encoder.Builder encoder() {
		return new Encoder.Builder();
	}

	public static Decoder.Builder decoder() {
		return new Decoder.Builder();
	}

	public static class Encoder {

		private static final Pattern PIN_PATTERN = Pattern.compile("\\d{4,12}");

		private static final char PIN_CONTROL_FIELD = '4';

		private static final String PIN_FILL_DIGIT = "A";

		private static final Pattern PAN_PATTERN = Pattern.compile("\\d{0,19}");

		private static final int PAN_BASE_LENGTH = 12;

		private static final String PAN_PAD_DIGIT = "0";

		private final RandomGenerator generator;

		private final MacCalculator cbcMacCalculator;

		private Encoder(RandomGenerator generator, MacCalculator cbcMacCalculator) {
			this.generator = generator;
			this.cbcMacCalculator = cbcMacCalculator;
		}

		public Encoder withRandomGenerator(RandomGenerator generator) {
			return new Encoder(generator, cbcMacCalculator);
		}

		public byte[] encode(CharSequence pin, CharSequence pan) {
			byte[] block = ByteBuffer.allocate(32) //
				.put(createPinField(pin)) //
				.put(createPanField(pan)) //
				.array();

			return cbcMacCalculator.calculate(block);
		}

		private byte[] createPinField(CharSequence pin) {
			if (!PIN_PATTERN.matcher(pin).matches()) {
				throw new IllegalArgumentException("Invalid PIN: " + pin);
			}

			int pinLength = pin.length();
			int fillingLength = 14 - pinLength;
			byte[] randomBytes = new byte[8];

			generator.nextBytes(randomBytes);

			// FIXME JDK toHexString() lacks leading zeros
			CharBuffer hex = CharBuffer.allocate(32)
				.append(PIN_CONTROL_FIELD)
				.append(Integer.toHexString(pinLength))
				.append(pin)
				.append(PIN_FILL_DIGIT.repeat(fillingLength))
				.append(Long.toHexString(ByteBuffer.wrap(randomBytes).getLong()));

			return HexFormat.parseHex(hex);
		}

		private static byte[] createPanField(CharSequence pan) {
			if (!PAN_PATTERN.matcher(pan).matches()) {
				throw new IllegalArgumentException("Invalid PAN: " + pan);
			}

			int panLength = pan.length();

			int leftPaddingLength = panLength < PAN_BASE_LENGTH ? PAN_BASE_LENGTH - panLength : 0;
			int rightPaddingLength = 31 - (leftPaddingLength + panLength);

			CharBuffer hex = CharBuffer.allocate(32)
				.append(Integer.toString(panLength < PAN_BASE_LENGTH ? 0 : panLength - PAN_BASE_LENGTH))
				.append(PAN_PAD_DIGIT.repeat(leftPaddingLength))
				.append(pan)
				.append(PAN_PAD_DIGIT.repeat(rightPaddingLength));

			return HexFormat.parseHex(hex);
		}

		public static class Builder {

			public Encoder withCBCMacMode(MacCalculator cbcMacCalculator) {
				return new Encoder(SecureRandom::new, cbcMacCalculator);
			}

			public Encoder withECBMode(Encryptor ecbEncryptor) {
				// FIXME suboptimal as the two subarrays already exist
				MacCalculator cbcMacCalculator = input -> {
					byte[] pinField = Arrays.copyOfRange(input, 0, input.length / 2);
					byte[] intermediateBlockA = ecbEncryptor.encrypt(pinField);
					byte[] panField = Arrays.copyOfRange(input, input.length / 2, input.length);
					byte[] intermediateBlockB = xor(intermediateBlockA, panField);
					return ecbEncryptor.encrypt(intermediateBlockB);
				};

				return new Encoder(SecureRandom::new, cbcMacCalculator);
			}

			private static byte[] xor(byte[] x1, byte[] x2) {
				return null; // FIXME
			}

		}

	}

	public static class Decoder {

		private final Decryptor ecbDecryptor;

		private Decoder(Decryptor ecbDecryptor) {
			this.ecbDecryptor = ecbDecryptor;
		}

		public CharBuffer decode(byte[] pinBlock, CharSequence pan) {
			return null; // FIXME
		}

		public static class Builder {

			public Decoder withECBMode(Decryptor ecbDecryptor) {
				return new Decoder(ecbDecryptor);
			}

		}

	}

}
