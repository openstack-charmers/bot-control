ini-splice
==============================================================================
```
usage: ini-splice [-h] [-0] [-v] ini_file action target from other_file

An INI configuration file parser. Currently only supports replacing entire
sections. Usage example: `ini-splice tox.ini replace-section testenv from
gold.ini`

positional arguments:
  ini_file     INI file to edit.
  action       Action to take. Currently supported actions: replace-section.
  target       Target on which to take action. Currently supported target
               types: section name.
  from         A required positional.
  other_file   INI file to read and use in the context of the action.

optional arguments:
  -h, --help   show this help message and exit
  -0, --dry    Dry run. Does not alter anything.
  -v, --debug  Enable verbose debug output.
```


#### Examples

```
# Clone the tool to a tools directory
mkdir -p /home/ubuntu/tools
git clone https://github.com/openstack-charmers/bot-control /home/ubuntu/tools/bot-control
cd /home/ubuntu/tools/bot-control/tools

# Replace the testenv section of tox.ini with that from gold.ini
./ini-splice tox.ini replace-section testenv from gold.ini -v
```
