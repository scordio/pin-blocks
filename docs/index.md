# Overview

PIN Blocks is a collection of encoders and decoders for ISO 9564-1 PIN block formats.

## Compatibility

PIN Blocks requires Java 11 or higher.

## Getting Started

[![PIN Blocks](https://img.shields.io/maven-central/v/io.github.scordio/pin-blocks?label=PIN%20Blocks&color=#4cae4f)](https://central.sonatype.com/artifact/io.github.scordio/pin-blocks)

=== ":simple-apachemaven: Maven"

    ``` xml
    <dependency>
      <groupId>io.github.scordio</groupId>
      <artifactId>pin-blocks</artifactId>
      <version>${pin-blocks.version}</version>
    </dependency>
    ```

=== ":simple-gradle: Gradle"

    ``` kotlin
    implementation("io.github.scordio:pin-blocks:${pinBlocksVersion}")
    ```

## Formats

The following formats are supported:

* [Format 0](formats/format-0.md), as per [ISO 9564-1][] specification
* [Format 1](formats/format-1.md), as per [ISO 9564-1:2002][] specification
* [Format 2](formats/format-2.md), as per [ISO 9564-3:2003][] specification
* [Format 3](formats/format-3.md), as per [ISO 9564-1:2002][] specification
* [Format 4](formats/format-4.md), as per [ISO 9564-1:2017][] specification

[ISO 9564-1]: https://www.iso.org/standard/17309.html
[ISO 9564-1:2002]: https://www.iso.org/standard/29374.html
[ISO 9564-3:2003]: https://www.iso.org/standard/35124.html
[ISO 9564-1:2017]: https://www.iso.org/standard/68669.html
