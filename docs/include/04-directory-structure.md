# Directory structure

Stampo expect some conventions. The minimal directory structure is:

```
.
```

(Yes, empty dir)

## Content directory

All the content that must be processed should be put in the `content` directory.

```
.
└── content/index.html.peb
```

Executing `stampo` in the root directory of the project will generate:

```
.
├── content/index.html.peb
|
└── output/index.html
```


## Configuration file

Stampo will read, if present, a configuration file named `configuration.yaml`.

It must be placed in the root directory of the project:

```
.
├── content/*
|
└── configuration.yaml
```

Currently in the configuration file you can specify the following properties:

- `locales` : a list of locales (can be one). When setting more than one locales, the website will be considered a multi language site and all the content will be generated for each locale in `<output>/[locale]/`
  
    Example: `locales: [en,de,fr,it]`

- `default-locale` : specify a default locale. When generating the content for the specified default locale, it will placed in `<output>/` directly instead of `<output>/[locale]/`

- `use-ugly-url` : By default false. By default, stampo will generate for a file named `test.html` the following file `test/index.html`. With `use-ugly-url` set to true, stampo will generate `test.html`. Note that you can set this value on a single file instead of applying it globally.

- `taxonomies` : TODO TBD


## Static directory

If you have files that don't need to be processed (images, ...), you can put them in the directory `static`. The content will be copied directly in the output directory.

For example, if you have:

```
.
├── content/index.html.peb
|
└── static/images/my-image.png
```

The output will be:

```
output
  |
  ├── index.html
  |
  └── images
         |
         └── my-image.png
```


## Layout directory

## Locales directory

## Data directory

## Final overview
  