port-cleanup
==============================================================================

```
usage: port-cleanup [-h] [-pn PORT_NAME_REGEX] [-ps PORT_STATUS] [-v] [-q]
                    [--delete]

A tool to clean up certain matching neutron ports, such as may be left behind
after failed overcloud deployment tests.

optional arguments:
  -h, --help            show this help message and exit
  -pn PORT_NAME_REGEX, --port-name-regex PORT_NAME_REGEX
                        Regex string to use to match port names
  -ps PORT_STATUS, --port-status PORT_STATUS
                        Status of port to match (exact string)
  -v, --debug           Enable verbose debug output
  -q, --quiet           Slightly less verbose, but not silent
  --delete              Delete matching ports (USE WITH CAUTION)
```


#### Examples
```
# Query and show all ports, but do not delete.
./port-cleanup

# Query and delete all matching ports.
./port-cleanup -pn "juju-osci-.*-machine.*ext-port" -ps "DOWN" --delete
```

```
# Query all matching ports but don't list each port and don't delete them.
# Shows that 811 of 900 ports matched and would have been deleted.
⟫ ./port-cleanup -pn "juju-osci.*-machine.*ext-port" -ps "DOWN" -q
INFO:root:Command arguments: {'port_status': 'DOWN', 'quiet': True, 'port_delete': False, 'debug': False, 'port_name_regex': 'juju-osci.*-machine.*ext-port'}
INFO:root:Keystone client region name check: True
INFO:root:Nova client flavor list check: 8
INFO:root:Neutron client network list check: 25
INFO:root:Neutron ports total: 900
INFO:root:Neutron ports matched: 811
INFO:root:Nova servers in ACTIVE status: 35
INFO:root:Nova servers in ERROR status: 0
```
