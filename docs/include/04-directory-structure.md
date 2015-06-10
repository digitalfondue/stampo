# Directory structure

Stampo expect some conventions. The minimal directory structure is:

```
.
└── content/
```

## Configuration file

Stampo will read, if present, a configuration file named `configuration.yaml`.

It must be placed in the root directory of the project:

```
.
├── content/
|
└── configuration.yaml
```

Currently in the configuration file you can specify the following properties:

- `locales` : a list of locales (can be one). When setting more than one locales, the website will be considered a multi language site and all the content will be generated for each locale in `<output>/[locale]/`
  
    Example: `locales: [en,de,fr,it]`

- `default-locale` : specify a default locale. When generating the content for the specified default locale, it will placed in `<output>/` directly instead of `<output>/[locale]/`

- `use-ugly-url` : By default false. By default, stampo will generate for a file named `test.html` the following file `test/index.html`. With `use-ugly-url` set to true, stampo will generate `test.html`. Note that you can set this value on a single file instead of applying it globally.

- `taxonomies` : TBD


## Static directory

## Layout directory

## Locales directory

## Data directory

## Final overview
  