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

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * This class consists exclusively of static methods for obtaining encoders and decoders
 * for the Format 4 PIN block, as specified in
 * <a href="https://www.iso.org/standard/68669.html">ISO 9564-1:2017</a>.
 */
public class Format4 {

	private static final int BLOCK_LENGTH_BYTES = 16;

	private static final Pattern PIN_PATTERN = Pattern.compile("\\d{4,12}");

	private static final char PIN_CONTROL_FIELD = '4';

	private static final int PIN_FILL_NIBBLE = 0x0A;

	private static final Pattern PAN_PATTERN = Pattern.compile("\\d{0,19}");

	private static final int PAN_BASE_LENGTH = 12;

	private Format4() {
	}

	/**
	 * Returns a builder for a Format 4 {@link Encoder Encoder}.
	 * @return the builder for a Format 4 encoder
	 */
	public static Encoder.Builder encoder() {
		return new Encoder.Builder();
	}

	/**
	 * Returns a builder for a Format 4 {@link Decoder Decoder}.
	 * @return the builder for a Format 4 decoder
	 */
	public static Decoder.Builder decoder() {
		return new Decoder.Builder();
	}

	private static byte[] createPanField(String pan) {
		Objects.requireNonNull(pan, "'pan' must not be null");

		if (!PAN_PATTERN.matcher(pan).matches()) {
			throw new IllegalArgumentException("Invalid PAN");
		}

		int panLength = pan.length();
		byte[] panField = new byte[BLOCK_LENGTH_BYTES];

		int controlNibble = panLength < PAN_BASE_LENGTH ? 0 : panLength - PAN_BASE_LENGTH;
		setNibble(panField, 0, controlNibble);

		int panStartIndex = 1 + Math.max(0, PAN_BASE_LENGTH - panLength);
		for (int i = 0; i < panLength; i++) {
			setNibble(panField, panStartIndex + i, decimalDigitOf(pan.charAt(i)));
		}

		return panField;
	}

	private static byte[] xor(byte[] b1, byte[] b2) {
		if (b1.length != b2.length) {
			throw new IllegalArgumentException("Mismatched block lengths");
		}

		byte[] result = new byte[b1.length];
		for (int i = 0; i < b1.length; i++) {
			result[i] = (byte) (b1[i] ^ b2[i]);
		}
		return result;
	}

	private static int decimalDigitOf(char digit) {
		int decimal = Character.digit(digit, 10);
		if (decimal < 0) {
			throw new IllegalArgumentException("Invalid numeric input");
		}
		return decimal;
	}

	private static int getNibble(byte[] source, int index) {
		int byteIndex = index / 2;
		int value = source[byteIndex] & 0xFF;
		return index % 2 == 0 ? value >>> 4 : value & 0x0F;
	}

	private static void setNibble(byte[] target, int index, int value) {
		int byteIndex = index / 2;
		int nibble = value & 0x0F;
		target[byteIndex] = index % 2 == 0 //
				? (byte) ((target[byteIndex] & 0x0F) | (nibble << 4)) //
				: (byte) ((target[byteIndex] & 0xF0) | nibble);
	}

	private static void clear(byte @Nullable [] value) {
		if (value != null) {
			Arrays.fill(value, (byte) 0x00);
		}
	}

	private static byte[] requireBlock(byte[] value) {
		if (value.length != BLOCK_LENGTH_BYTES) {
			throw new IllegalArgumentException("Invalid block length");
		}
		return value;
	}

	/**
	 * Encoder for the Format 4 PIN block.
	 * <p>
	 * Requires an {@link Encryptor} that uses AES in ECB mode, and, optionally, a
	 * {@link RandomGenerator}.
	 * <p>
	 * {@link Encoder} instances are thread-safe as long as the supplied {@link Encryptor}
	 * and {@link RandomGenerator} are also thread-safe.
	 */
	public static class Encoder {

		private final RandomGenerator generator;

		private final Encryptor encryptor;

		private Encoder(RandomGenerator generator, Encryptor encryptor) {
			this.generator = Objects.requireNonNull(generator, "'generator' must not be null");
			this.encryptor = Objects.requireNonNull(encryptor, "'encryptor' must not be null");
		}

		/**
		 * Returns an encoder instance that encrypts equivalently to this one, but uses
		 * the given random generator instead of the default one based on
		 * {@link SecureRandom}.
		 * @param generator the random generator to use; never {@code null}
		 * @return an equivalent encoder that uses the given random generator
		 */
		public Encoder withRandomGenerator(RandomGenerator generator) {
			return new Encoder(generator, encryptor);
		}

		/**
		 * Encodes the given PIN and PAN in a PIN block.
		 * @param pin the PIN to encode, consisting of 4 to 12 digits; never {@code null}
		 * @param pan the PAN associated with the PIN, consisting of up to 19 digits;
		 * never {@code null}
		 * @return the encoded PIN block
		 */
		public byte[] encode(CharSequence pin, String pan) {
			byte[] pinField = null;
			byte[] panField = null;
			byte[] intermediateBlockA = null;
			byte[] intermediateBlockB = null;

			try {
				pinField = createPinField(pin);
				intermediateBlockA = requireBlock(encryptor.encrypt(pinField));
				panField = createPanField(pan);
				intermediateBlockB = xor(intermediateBlockA, panField);
				return requireBlock(encryptor.encrypt(intermediateBlockB));
			}
			finally {
				clear(pinField);
				clear(panField);
				clear(intermediateBlockA);
				clear(intermediateBlockB);
			}
		}

		private byte[] createPinField(CharSequence pin) {
			Objects.requireNonNull(pin, "'pin' must not be null");

			if (!PIN_PATTERN.matcher(pin).matches()) {
				throw new IllegalArgumentException("Invalid PIN");
			}

			int pinLength = pin.length();
			byte[] randomBytes = new byte[BLOCK_LENGTH_BYTES / 2];
			byte[] pinField = new byte[BLOCK_LENGTH_BYTES];

			try {
				generator.nextBytes(randomBytes);

				setNibble(pinField, 0, Character.digit(PIN_CONTROL_FIELD, 16));
				setNibble(pinField, 1, pinLength);

				int nibble = 2;
				for (int i = 0; i < pinLength; i++) {
					setNibble(pinField, nibble++, decimalDigitOf(pin.charAt(i)));
				}
				for (; nibble < BLOCK_LENGTH_BYTES; nibble++) {
					setNibble(pinField, nibble, PIN_FILL_NIBBLE);
				}
				System.arraycopy(randomBytes, 0, pinField, BLOCK_LENGTH_BYTES / 2, BLOCK_LENGTH_BYTES / 2);
				return pinField;
			}
			finally {
				clear(randomBytes);
			}
		}

		/**
		 * A builder for {@link Encoder} instances.
		 */
		public static class Builder {

			private Builder() {
			}

			/**
			 * Creates a new encoder that uses the given encryptor and a random generator
			 * based on {@link SecureRandom}.
			 * <p>
			 * The encryptor must use AES in ECB mode, operating on 16-byte (128-bit)
			 * blocks.
			 * <p>
			 * To override the random generator, use
			 * {@link Encoder#withRandomGenerator(RandomGenerator)}.
			 * @param encryptor the encryptor to use; never {@code null}
			 * @return the configured encoder
			 */
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

	/**
	 * Decoder for the Format 4 PIN block.
	 * <p>
	 * Requires a {@link Decryptor} that uses AES in ECB mode.
	 * <p>
	 * {@link Decoder} instances are thread-safe as long as the supplied {@link Decryptor}
	 * is also thread-safe.
	 */
	public static class Decoder {

		private final Decryptor decryptor;

		private Decoder(Decryptor decryptor) {
			this.decryptor = Objects.requireNonNull(decryptor, "'decryptor' must not be null");
		}

		/**
		 * Decodes the given Format 4 PIN block with the given PAN, returning the PIN
		 * value.
		 * @param pinBlock the PIN block to decode; never {@code null}
		 * @param pan the PAN associated with the PIN block, consisting of up to 19
		 * digits; never {@code null}
		 * @return the decoded PIN
		 */
		public char[] decode(byte[] pinBlock, String pan) {
			requireBlock(pinBlock);

			byte[] intermediateBlockB = null;
			byte[] panField = null;
			byte[] intermediateBlockA = null;
			byte[] pinField = null;

			try {
				intermediateBlockB = requireBlock(decryptor.decrypt(pinBlock));
				panField = createPanField(pan);
				intermediateBlockA = xor(intermediateBlockB, panField);
				pinField = requireBlock(decryptor.decrypt(intermediateBlockA));
				return getPin(pinField);
			}
			finally {
				clear(intermediateBlockB);
				clear(panField);
				clear(intermediateBlockA);
				clear(pinField);
			}
		}

		private static char[] getPin(byte[] pinField) {
			validatePinControlField(pinField);
			int pinLength = getPinLength(pinField);
			validatePinFillDigits(pinLength, pinField);

			char[] pin = new char[pinLength];

			try {
				for (int i = 0; i < pin.length; i++) {
					int digit = getNibble(pinField, 2 + i);
					if (digit > 9) {
						throw new IllegalArgumentException("Invalid PIN digit");
					}
					pin[i] = Character.forDigit(digit, 10);
				}
				return pin;
			}
			catch (RuntimeException ex) {
				Arrays.fill(pin, '\0');
				throw ex;
			}
		}

		private static void validatePinControlField(byte[] pinField) {
			int pinControlField = getNibble(pinField, 0);
			if (pinControlField != Character.digit(PIN_CONTROL_FIELD, 16)) {
				throw new IllegalArgumentException("Invalid PIN control field");
			}
		}

		private static int getPinLength(byte[] pinField) {
			int pinLength = getNibble(pinField, 1);
			if (pinLength < 4 || pinLength > 12) {
				throw new IllegalArgumentException("Invalid PIN length");
			}
			return pinLength;
		}

		private static void validatePinFillDigits(int pinLength, byte[] pinField) {
			for (int i = 2 + pinLength; i < BLOCK_LENGTH_BYTES; i++) {
				if (getNibble(pinField, i) != PIN_FILL_NIBBLE) {
					throw new IllegalArgumentException("Invalid fill digits");
				}
			}
		}

		/**
		 * A builder for {@link Decoder} instances.
		 */
		public static class Builder {

			private Builder() {
			}

			/**
			 * Creates a new decoder that uses the given decryptor.
			 * <p>
			 * The decryptor must use AES in ECB mode, operating on 16-byte (128-bit)
			 * blocks.
			 * @param decryptor the decryptor to use; never {@code null}
			 * @return the configured decoder
			 */
			public Decoder withDecryptor(Decryptor decryptor) {
				return new Decoder(decryptor);
			}

		}

	}

}
