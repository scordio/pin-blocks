# ISO 9564 Format 3

!!! warning

    This format has not yet been implemented.

Defined in [ISO 9564-1:2002](https://www.iso.org/standard/29374.html), this format is constructed like [Format 0](iso9564-format-0.md), by modulo-2 addition of two 64-bit fields: the plain text PIN field and the account number field. The differences are that the first nibble, which identifies the format, has the value 3, and that the fill digits are random values from 10 to 15 (`0xA`-`0xF`) instead of the fixed value 15 (`0xF`).
