# Overview

PIN Blocks is a collection of encoders and decoders for PIN block formats.

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

* [ISO 9564 Format 4](formats/iso9564-format-4.md)
