# Usage

Stampo has the following sub commands:

- [build](#build)
- [serve](#serve)
- [check](#check)
- [help](#help)

## Build

```sh
$ stampo
# the current directory will be built and put into ./output
```
or

```sh
$ stampo build
```

You can specify the source and destination directory with the option --src and 
--dist

For example:

```sh
$ stampo build --src=./my/project --dist=./my/custom-output
```

**Beware** the output directory will be cleaned up. So only use `--dist` if you know what you are doing!

## Serve

Stampo has an embedded web server. If your page has a &lt;head> element, it will append a script for automatically reload the page on any change.

```sh
$ stampo serve
```

## Check

You can check if the build will work correctly without touching the filesystem with:

```sh
$ stampo check
```

Stampo will do a build in memory and print a report of the generated files.

## Help

Display the help with

```
$ stampo help
```