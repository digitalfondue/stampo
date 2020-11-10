Stampo
======

A static website/documentation generator with an emphasis on multi language website. 
[![Build Status](https://img.shields.io/github/workflow/status/digitalfondue/stampo/Java%20CI%20with%20Maven)](https://github.com/digitalfondue/stampo/actions?query=workflow%3A%22Java+CI+with+Maven%22)

**Require Java8**

## Download

A full distribution with all the dependencies and shell/bat script is available in maven central.

Current release is stampo 1.2.1 [Download zip](https://repo1.maven.org/maven2/ch/digitalfondue/stampo/stampo/1.2.1/stampo-1.2.1.zip)

## Install

Add in your PATH the bin directory. Or, if you move the shell/bat script, adjust the paths to the lib/stampo.jar accordingly.

## Use

In your stampo project, for processing the site:

```sh
stampo
```

For running the embedded web server (it will listen to localhost:8080):

```sh
stampo serve
```

For checking the correctness:

```sh
stampo check
```

For visualizing the help:

```sh
stampo help
```

## As a maven dependency

If you want to include stampo as a library, it's available from maven central:

```XML
<dependency>
	<groupId>ch.digitalfondue.stampo</groupId>
	<artifactId>stampo</artifactId>
	<version>1.2.2</version>
</dependency>
```

## As a maven plugin

Stampo is available as a maven plugin, see https://github.com/digitalfondue/stampo-maven-plugin .

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
