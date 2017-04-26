```
Usage: lolo-log-puller [options] FILENAME [OPTIONAL ENV VAR LIST]

lolo-log-puller
==============================================================================
A simple tool to collect log files from Juju all application units in the
current Juju model.

Usage examples:
    ./lolo-log-puller /tmp/log_dir

    ./lolo-log-puller $WORKSPACE/logs

    ./lolo-log-puller $(mktemp -d)

    ./lolo-log-puller -v0


Options:
  -h, --help       show this help message and exit
  -d, -v, --debug  Enable debug logging.  (Default: False)
  -0, --dry-run    Dry run, do not actually do anything. (Default: False)

```
