```Usage: env-render [options] FILENAME [OPTIONAL ENV VAR LIST]

env-render
==============================================================================
Inject environment variables into a file using jinja2-style template syntax.

Usage examples:

  Given the following FILENAME contents (ie. template):
    Dear {{ env['USER'] or "Ubuntu user" }},
    My favorite type of bacon is {{ env['FAV_BACON'] or "any bacon" }}.
    See you at {{ env['HOME'] or "home" }}.
    <eof>

  Expand all matches using all available environment variables:
    ./env-render FILENAME

  Expand all ${} matches using only the listed environment variables:
    ./env-render FILENAME "HOME,USER"


Options:
  -h, --help       show this help message and exit
  -d, -v, --debug  Enable debug logging.  (Default: False)
  -y, --confirm    Required to overwrite.  (Default: False)
```
