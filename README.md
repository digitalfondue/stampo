Stampo
======

A static website/documentation generator with an emphasis on multi language website. [![Build Status](https://travis-ci.org/digitalfondue/stampo.svg?branch=master)](https://travis-ci.org/digitalfondue/stampo) [![Coverage Status](https://coveralls.io/repos/digitalfondue/stampo/badge.svg?branch=master)](https://coveralls.io/r/digitalfondue/stampo?branch=master)

**Require Java8**

## Download

A full distribution with all the dependencies and shell/bat script is available in maven central.

Current release is stampo 1.1. [Download zip](https://central.maven.org/maven2/ch/digitalfondue/stampo/stampo/1.1/stampo-1.1.zip)

## As a maven dependency

If you want to include stampo as a library, it's available from maven central:

```
<dependency>
	<groupId>ch.digitalfondue.stampo</groupId>
	<artifactId>stampo</artifactId>
	<version>1.1</version>
</dependency>
```

## Features

 - easy multi language support
 - support for taxonomies
 - pagination over "pages" (for blog posts), over taxonomy values, over "static content" (for galleries).
 - draft support
 - support multiple template/rendering engine: markdown, pebble template, freemarker
 - support for recursive inclusion and structured pages, with Table of Contents extracted from the html: useful for documentations
 - layout system
 - embedded web server with auto reload on change

## TODO

Documentation will come ASAP :).

Tested only on linux. 

You can see an example at : https://github.com/nyx-arch/nyx-arch.github.io/tree/stampo-content . The master contain the output.
