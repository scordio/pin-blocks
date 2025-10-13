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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.github.scordio.tests.pinblocks.iso9564.Cipher.AES_ECB;
import static org.assertj.core.api.BDDAssertions.then;

class Format4Tests {

	@ParameterizedTest
	@CsvSource({ //
			"123456, 000000" //
	})
	void encode(CharSequence pin, String pan) {
		// Given
		Encoder underTest = Format4.encoder().withEncryptor(AES_ECB);

		// When
		byte[] pinBlock = underTest.encode(pin, pan);

		// Then
		then(pinBlock).hasSize(16);
	}

	@ParameterizedTest
	@CsvSource({ //
			"67153CBAA99D8D53ABD15C45C8CEAB01, 000000, 123456", //
			"F7542CEDC3EE4435CE43A2CAC8B26A19, 000000, 123456" //
	})
	void decode(@Hex byte[] pinBlock, String pan, String expected) {
		// Given
		Decoder underTest = Format4.decoder().withDecryptor(AES_ECB);

		// When
		CharSequence pin = underTest.decode(pinBlock, pan);

		// Then
		then(pin.toString()).isEqualTo(expected);
	}

}
