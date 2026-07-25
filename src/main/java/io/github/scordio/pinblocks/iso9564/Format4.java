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

import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Format4 {

	private static final Pattern PIN_PATTERN = Pattern.compile("\\d{4,12}");

	private static final char PIN_CONTROL_FIELD = '4';

	private static final String PIN_FILL_DIGIT = "A";

	private static final Pattern PAN_PATTERN = Pattern.compile("\\d{0,19}");

	private static final int PAN_BASE_LENGTH = 12;

	private static final String PAN_PAD_DIGIT = "0";

	private Format4() {
	}

	public static Encoder.Builder encoder() {
		return new Encoder.Builder();
	}

	public static Decoder.Builder decoder() {
		return new Decoder.Builder();
	}

	private static String createPinFillDigits(int pinLength) {
		return PIN_FILL_DIGIT.repeat(14 - pinLength);
	}

	private static byte[] createPanField(String pan) {
		if (!PAN_PATTERN.matcher(pan).matches()) {
			throw new IllegalArgumentException("Invalid PAN: " + pan);
		}

		int panLength = pan.length();

		String controlField = Integer.toString(panLength < PAN_BASE_LENGTH ? 0 : panLength - PAN_BASE_LENGTH);
		int leftPaddingLength = panLength < PAN_BASE_LENGTH ? PAN_BASE_LENGTH - panLength : 0;
		int rightPaddingLength = 31 - (leftPaddingLength + panLength);

		CharBuffer hex = CharBuffer.allocate(32)
			.append(controlField)
			.append(PAN_PAD_DIGIT.repeat(leftPaddingLength))
			.append(pan)
			.append(PAN_PAD_DIGIT.repeat(rightPaddingLength))
			.flip();

		return HexFormat.parseHex(hex);
	}

	private static byte[] xor(byte[] b1, byte[] b2) {
		byte[] result = new byte[b1.length];
		for (int i = 0; i < b1.length; i++) {
			result[i] = (byte) (b1[i] ^ b2[i]);
		}
		return result;
	}

	public static class Encoder {

		private final RandomGenerator generator;

		private final Encryptor encryptor;

		private Encoder(RandomGenerator generator, Encryptor encryptor) {
			this.generator = generator;
			this.encryptor = encryptor;
		}

		public Encoder withRandomGenerator(RandomGenerator generator) {
			return new Encoder(generator, encryptor);
		}

		public byte[] encode(CharSequence pin, String pan) {
			byte[] intermediateBlockA = encryptor.encrypt(createPinField(pin));
			byte[] intermediateBlockB = xor(intermediateBlockA, createPanField(pan));
			return encryptor.encrypt(intermediateBlockB);
		}

		private byte[] createPinField(CharSequence pin) {
			if (!PIN_PATTERN.matcher(pin).matches()) {
				throw new IllegalArgumentException("Invalid PIN: " + pin);
			}

			int pinLength = pin.length();
			byte[] randomBytes = new byte[8];

			generator.nextBytes(randomBytes);

			CharBuffer hex = CharBuffer.allocate(32)
				.append(PIN_CONTROL_FIELD)
				.append(Integer.toHexString(pinLength))
				.append(pin)
				.append(createPinFillDigits(pinLength))
				.put(HexFormat.formatHex(randomBytes))
				.flip();

			return HexFormat.parseHex(hex);
		}

		public static class Builder {

			public Builder() {
			}

			public Encoder withEncryptor(Encryptor encryptor) {
				return new Encoder(new DefaultRandomGenerator(), encryptor);
			}

			private static class DefaultRandomGenerator implements RandomGenerator {

				private final SecureRandom secureRandom = new SecureRandom();

				@Override
				public void nextBytes(byte[] bytes) {
					secureRandom.nextBytes(bytes);
				}

			}

		}

	}

	public static class Decoder {

		private final Decryptor decryptor;

		private Decoder(Decryptor decryptor) {
			this.decryptor = decryptor;
		}

		public char[] decode(byte[] pinBlock, String pan) {
			byte[] intermediateBlockB = decryptor.decrypt(pinBlock);
			byte[] intermediateBlockA = xor(intermediateBlockB, createPanField(pan));
			byte[] pinField = decryptor.decrypt(intermediateBlockA);

			char[] hex = HexFormat.formatHex(pinField);

			validatePinControlField(hex);
			int pinLength = getPinLength(hex);
			validatePinFillDigits(hex, pinLength);

			return Arrays.copyOfRange(hex, 2, 2 + pinLength);
		}

		private static void validatePinControlField(char[] hex) {
			if (hex[0] != PIN_CONTROL_FIELD) {
				throw new IllegalArgumentException("Invalid PIN control field: " + hex[0]);
			}
		}

		private static int getPinLength(char[] hex) {
			int pinLength = Character.digit(hex[1], 16);
			if (pinLength < 4 || pinLength > 12) {
				throw new IllegalArgumentException("Invalid PIN length: " + pinLength);
			}
			return pinLength;
		}

		private static void validatePinFillDigits(char[] hex, int pinLength) {
			String fillDigits = new String(hex, 2 + pinLength, 14 - pinLength);
			if (!fillDigits.equals(createPinFillDigits(pinLength))) {
				throw new IllegalArgumentException("Invalid fill digits: " + fillDigits);
			}
		}

		public static class Builder {

			public Builder() {
			}

			public Decoder withDecryptor(Decryptor decryptor) {
				return new Decoder(decryptor);
			}

		}

	}

}
