# ISO 9564 Format 4

Defined in [ISO 9564-1:2017](https://www.iso.org/standard/68669.html), this format is an extended PIN block format,
constructed using two 128-bit fields of PIN and PAN data, respectively.
This format is intended for use with a 128-bit block cipher such as AES, unlike formats 0 to 3, which suit 64-bit block
ciphers such as Triple DES.

## Cipher

Both the encoder and the decoder are built on the `Encryptor` and `Decryptor` abstractions, which allow plugging in any
cryptographic backend (Java Cryptography Architecture, BouncyCastle, an HSM client, etc.).
The library never handles keys itself.

For Format 4, the given `Encryptor` and `Decryptor` must use AES in ECB mode without padding, and operate on 16-byte
blocks.
For example, using JCA:

``` java
import io.github.scordio.pinblocks.Decryptor;
import io.github.scordio.pinblocks.Encryptor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

SecretKey key = new SecretKeySpec(keyBytes, "AES");

Encryptor encryptor = input -> {
  try {
    Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    return cipher.doFinal(input);
  }
  catch (GeneralSecurityException ex) {
    throw new IllegalStateException(ex);
  }
};

Decryptor decryptor = input -> {
  try {
    Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, key);
    return cipher.doFinal(input);
  }
  catch (GeneralSecurityException ex) {
    throw new IllegalStateException(ex);
  }
};
```

## Encoder

An encoder is obtained from `Format4.encoder()` by supplying an `Encryptor`:

``` java
import io.github.scordio.pinblocks.iso9564.Format4;
import io.github.scordio.pinblocks.iso9564.Format4.Encoder;

Encoder encoder = Format4.encoder().withEncryptor(encryptor);

byte[] pinBlock = encoder.encode("1234", "5555444433332222");
```

The PIN must consist of 4 to 12 digits and the PAN of up to 19 digits; otherwise, an `IllegalArgumentException` is
thrown. The resulting PIN block is 16 bytes long.

The 8 random bytes of the PIN field are generated with `SecureRandom` by default.
A different source of randomness can be provided with `withRandomGenerator`, for example to obtain reproducible output
in tests:

``` java
import io.github.scordio.pinblocks.iso9564.Format4;
import io.github.scordio.pinblocks.iso9564.Format4.Encoder;

import java.util.Arrays;

Encoder encoder = Format4.encoder()
  .withEncryptor(encryptor)
  .withRandomGenerator(bytes -> Arrays.fill(bytes, (byte) 0x42));
```

!!! warning

    Never use a predictable random generator in production: the random bytes are what makes two PIN blocks for the same
    PIN and PAN different.

Encoder instances are thread-safe as long as the supplied `Encryptor` and `RandomGenerator` are.

## Decoder

A decoder is obtained from `Format4.decoder()` by supplying a `Decryptor`:

``` java
import io.github.scordio.pinblocks.iso9564.Format4;
import io.github.scordio.pinblocks.iso9564.Format4.Decoder;

Decoder decoder = Format4.decoder().withDecryptor(decryptor);

char[] pin = decoder.decode(pinBlock, "5555444433332222");
```

The PIN is returned as a `char[]`, so that it can be cleared as soon as it is no longer needed.

An `IllegalArgumentException` is thrown if the PIN block is not 16 bytes long or if its decrypted content is not a valid
Format 4 PIN block.
Since the format has no integrity check, decoding a PIN block with the wrong PAN may still succeed and return a wrong
PIN.

Decoder instances are thread-safe as long as the supplied `Decryptor` is.
