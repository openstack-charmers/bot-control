```
Usage: protect-images --protect --admin

protect-images
==============================================================================
A tool to protect images in use. Used in combination with simplestreams which
will attempt to prune images without regard to use. The script will set
protected=True for all images that are in current use and set protected=False
for all images not in current use.

Usage examples:

  Test as non-admin (non-destructive)
    ./protect-images

  Non-destructive query only as admin
    ./protect-images --admin

  Set protected=True on images in use and protected=False on images not in use.
    ./protect-images --admin --delete


Options:
  -h, --help   show this help message and exit
  -v, --debug  Enable verbose debug output
  -q, --quiet  Slightly less verbose, but not silent
  --protect    Protect images in use. Note: --admin switch is also required to
               execute protect updates. (USE WITH CAUTION)
  --admin      Set administrator. Required to execute updates with the
               --protect switch (USE WITH CAUTION). Note: non-admin queries
               will underestimate the number of images to be protected due to
               limited visibility into images in use.

```
