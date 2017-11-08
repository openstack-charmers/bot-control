```
Usage: image-duplicate-cleanup --delete --admin

image-duplicate-cleanup
==============================================================================
A tool to delete duplicate glance images that are not in current use.

Usage examples:

  Test as non-admin (non-destructive)
    ./image-duplicate-cleanup

  Non-destructive query only as admin
    ./image-duplicate-cleanup --admin

  Delete duplicate images not in use
    ./image-duplicate-cleanup --admin --delete

Options:
  -h, --help   show this help message and exit
  -v, --debug  Enable verbose debug output
  -q, --quiet  Slightly less verbose, but not silent
  --delete     Delete duplicate images not in use. Note: --admin switch is
               also required to execute deletes. (USE WITH CAUTION)
  --admin      Set administrator. Required to execute deletes with the
               --delete switch (USE WITH CAUTION). Note: non-admin queries
               will overestimate the number of deletes queued due to limited
               visibility into images in use.

```
