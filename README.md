# Project Documentation

[![Build Status](https://api.travis-ci.org/typesafehub/project-doc.png?branch=master)](https://travis-ci.org/typesafehub/project-doc)

A general purpose project documentation website.

## Setting up development environment
The project is using [sbt-stylus](https://github.com/huntc/sbt-stylus) which runs only with Node. Install node on your machine:
```
brew install node
```

Also the project is using [sbt-sass](https://github.com/ShaggyYeti/sbt-sass) to compile Sass files to CSS. This plugin needs the Saas compiler and compass to work:
```
gem install sass
gem install compass
```

Afterwards you can start the Play application locally:
```
sbt run
```