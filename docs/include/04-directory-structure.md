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

Stampo has a layout mechanism for sharing common parts (TODO: see layout chapter). An override mechanism is present for selecting the layout given the page to render.

For example if you have:


```
.
├── content/
|      |
|      ├── index.md
|      |
|      └── post/my-post.md
|    
└── layout/
      |
      ├── index.html.peb
      |
      └── post/index.html.peb
```

To `content/index.md` the layout `layout/index.html.peb` will be applied and to `content/post/my-post.md` the layout `layout/post/index.html`. The rule for selecting the layout is:

 - is there a layout named `layout/post/my-post.html.*` (we mirror the directories name between content and layout)? If yes, use it.
 - else, check if `layout/post/index.html.*` is present ? If yes use it.
 - else go back to the parent directory and check if a layout index.html.* is present? If yes use it or else repeat this rule until the layout dir is reached and there is no layout available 
 
The rendered content will be exported as a variable named `content` in the template.

## Locales directory

## Data directory

## Final overview
  