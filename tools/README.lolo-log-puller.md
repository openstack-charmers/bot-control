## Usage

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


## Example (with a known failed unit)

```
rbeisner@rby:~/git/bot-control/tools⟫ juju status
Model  Controller  Cloud/Region         Version
foo    lxd         localhost/localhost  2.2-beta2

App  Version  Status  Scale  Charm   Store       Rev  OS      Notes
bar  16.04    active    1/2  ubuntu  jujucharms   10  ubuntu
foo  16.04    active      3  ubuntu  jujucharms   10  ubuntu

Unit    Workload  Agent  Machine  Public address  Ports  Message
bar/0*  active    idle   3        10.0.9.107             ready
bar/1   unknown   lost   4        10.0.9.31              agent lost, see 'juju show-status-log bar/1'
foo/0*  active    idle   0        10.0.9.150             ready
foo/1   active    idle   1        10.0.9.21              ready
foo/2   active    idle   2        10.0.9.161             ready

Machine  State    DNS         Inst id        Series  AZ  Message
0        started  10.0.9.150  juju-c94af0-0  xenial      Running
1        started  10.0.9.21   juju-c94af0-1  xenial      Running
2        started  10.0.9.161  juju-c94af0-2  xenial      Running
3        started  10.0.9.107  juju-c94af0-3  xenial      Running
4        down     10.0.9.31   juju-c94af0-4  xenial      Stopped
```

```
rbeisner@rby:~/git/bot-control/tools⟫ ./lolo-log-puller /tmp/log_dir
INFO:root:Application units: ['bar/0', 'bar/1', 'foo/0', 'foo/1', 'foo/2']
INFO:root:cmd [returned 0]: juju ssh bar/0 sudo tar -cjf /home/ubuntu/bar-0-var-log.tar.bz2 /var/log --warning=no-file-changed >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh bar/0 sudo tar -cjf /home/ubuntu/bar-0-etc.tar.bz2 /etc --warning=no-file-changed --exclude="/etc/X11" --exclude="/etc/ssl" --exclude="/etc/ssh" --exclude="shadow" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh bar/0 "[[ -d /var/lib/charm ]] && sudo tar -cjf /home/ubuntu/bar-0-var-lib-charm.tar.bz2 /var/lib/charm ||:" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh bar/0 "dpkg -l | bzip2 -9z > /home/ubuntu/bar-0-dpkg-list.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh bar/0 "df -h | bzip2 -9z > /home/ubuntu/bar-0-df.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh bar/0 "ps aux | bzip2 -9z > /home/ubuntu/bar-0-processes.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh bar/0 "sudo netstat -taupn | grep LISTEN | bzip2 -9z > /home/ubuntu/bar-0-listening.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju scp bar/0:/home/ubuntu/*.bz2 /tmp/log_dir >/dev/null 2>&1
INFO:root:cmd [returned 256]: juju ssh bar/1 sudo tar -cjf /home/ubuntu/bar-1-var-log.tar.bz2 /var/log --warning=no-file-changed >/dev/null 2>&1
ERROR:root:Failed to get logs from application unit: bar/1
INFO:root:cmd [returned 0]: juju ssh foo/0 sudo tar -cjf /home/ubuntu/foo-0-var-log.tar.bz2 /var/log --warning=no-file-changed >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/0 sudo tar -cjf /home/ubuntu/foo-0-etc.tar.bz2 /etc --warning=no-file-changed --exclude="/etc/X11" --exclude="/etc/ssl" --exclude="/etc/ssh" --exclude="shadow" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/0 "[[ -d /var/lib/charm ]] && sudo tar -cjf /home/ubuntu/foo-0-var-lib-charm.tar.bz2 /var/lib/charm ||:" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/0 "dpkg -l | bzip2 -9z > /home/ubuntu/foo-0-dpkg-list.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/0 "df -h | bzip2 -9z > /home/ubuntu/foo-0-df.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/0 "ps aux | bzip2 -9z > /home/ubuntu/foo-0-processes.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/0 "sudo netstat -taupn | grep LISTEN | bzip2 -9z > /home/ubuntu/foo-0-listening.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju scp foo/0:/home/ubuntu/*.bz2 /tmp/log_dir >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/1 sudo tar -cjf /home/ubuntu/foo-1-var-log.tar.bz2 /var/log --warning=no-file-changed >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/1 sudo tar -cjf /home/ubuntu/foo-1-etc.tar.bz2 /etc --warning=no-file-changed --exclude="/etc/X11" --exclude="/etc/ssl" --exclude="/etc/ssh" --exclude="shadow" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/1 "[[ -d /var/lib/charm ]] && sudo tar -cjf /home/ubuntu/foo-1-var-lib-charm.tar.bz2 /var/lib/charm ||:" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/1 "dpkg -l | bzip2 -9z > /home/ubuntu/foo-1-dpkg-list.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/1 "df -h | bzip2 -9z > /home/ubuntu/foo-1-df.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/1 "ps aux | bzip2 -9z > /home/ubuntu/foo-1-processes.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/1 "sudo netstat -taupn | grep LISTEN | bzip2 -9z > /home/ubuntu/foo-1-listening.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju scp foo/1:/home/ubuntu/*.bz2 /tmp/log_dir >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/2 sudo tar -cjf /home/ubuntu/foo-2-var-log.tar.bz2 /var/log --warning=no-file-changed >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/2 sudo tar -cjf /home/ubuntu/foo-2-etc.tar.bz2 /etc --warning=no-file-changed --exclude="/etc/X11" --exclude="/etc/ssl" --exclude="/etc/ssh" --exclude="shadow" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/2 "[[ -d /var/lib/charm ]] && sudo tar -cjf /home/ubuntu/foo-2-var-lib-charm.tar.bz2 /var/lib/charm ||:" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/2 "dpkg -l | bzip2 -9z > /home/ubuntu/foo-2-dpkg-list.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/2 "df -h | bzip2 -9z > /home/ubuntu/foo-2-df.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/2 "ps aux | bzip2 -9z > /home/ubuntu/foo-2-processes.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju ssh foo/2 "sudo netstat -taupn | grep LISTEN | bzip2 -9z > /home/ubuntu/foo-2-listening.bz2" >/dev/null 2>&1
INFO:root:cmd [returned 0]: juju scp foo/2:/home/ubuntu/*.bz2 /tmp/log_dir >/dev/null 2>&1
```

```
rbeisner@rby:~/git/bot-control/tools⟫ ls -alh /tmp/log_dir/
total 4.9M
drwxrwxr-x  2 rbeisner rbeisner 4.0K Apr 26 17:41 .
drwxrwxrwt 28 root     root     3.1M Apr 26 17:41 ..
-rw-rw-r--  1 rbeisner rbeisner  270 Apr 26 17:40 bar-0-df.bz2
-rw-rw-r--  1 rbeisner rbeisner  13K Apr 26 17:40 bar-0-dpkg-list.bz2
-rw-r--r--  1 rbeisner rbeisner 383K Apr 26 17:40 bar-0-etc.tar.bz2
-rw-rw-r--  1 rbeisner rbeisner  107 Apr 26 17:40 bar-0-listening.bz2
-rw-rw-r--  1 rbeisner rbeisner 1.1K Apr 26 17:40 bar-0-processes.bz2
-rw-r--r--  1 rbeisner rbeisner  48K Apr 26 17:40 bar-0-var-log.tar.bz2
-rw-rw-r--  1 rbeisner rbeisner  270 Apr 26 17:40 foo-0-df.bz2
-rw-rw-r--  1 rbeisner rbeisner  13K Apr 26 17:40 foo-0-dpkg-list.bz2
-rw-r--r--  1 rbeisner rbeisner 383K Apr 26 17:40 foo-0-etc.tar.bz2
-rw-rw-r--  1 rbeisner rbeisner  109 Apr 26 17:40 foo-0-listening.bz2
-rw-rw-r--  1 rbeisner rbeisner 1.1K Apr 26 17:40 foo-0-processes.bz2
-rw-r--r--  1 rbeisner rbeisner  47K Apr 26 17:40 foo-0-var-log.tar.bz2
-rw-rw-r--  1 rbeisner rbeisner  272 Apr 26 17:41 foo-1-df.bz2
-rw-rw-r--  1 rbeisner rbeisner  13K Apr 26 17:41 foo-1-dpkg-list.bz2
-rw-r--r--  1 rbeisner rbeisner 383K Apr 26 17:41 foo-1-etc.tar.bz2
-rw-rw-r--  1 rbeisner rbeisner  109 Apr 26 17:41 foo-1-listening.bz2
-rw-rw-r--  1 rbeisner rbeisner 1.1K Apr 26 17:41 foo-1-processes.bz2
-rw-r--r--  1 rbeisner rbeisner  47K Apr 26 17:41 foo-1-var-log.tar.bz2
-rw-rw-r--  1 rbeisner rbeisner  270 Apr 26 17:41 foo-2-df.bz2
-rw-rw-r--  1 rbeisner rbeisner  13K Apr 26 17:41 foo-2-dpkg-list.bz2
-rw-r--r--  1 rbeisner rbeisner 383K Apr 26 17:41 foo-2-etc.tar.bz2
-rw-rw-r--  1 rbeisner rbeisner  108 Apr 26 17:41 foo-2-listening.bz2
-rw-rw-r--  1 rbeisner rbeisner 1.1K Apr 26 17:41 foo-2-processes.bz2
-rw-r--r--  1 rbeisner rbeisner  48K Apr 26 17:41 foo-2-var-log.tar.bz2
```
