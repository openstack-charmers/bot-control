```
bundle-reducer
==============================================================================
A tool to extract subsets of juju-deployer bundle yaml files.

Default behavior:
  Reduce a bundle to the specified services, including all of the directly-
  related services, and save to an out_nnnnnn.yaml file in the current dir.

Usage examples:
  Reduce to only ceilometer, with all of the directly-related services.  Save
  to a new auto-generated filename in the current dir.
      ./bundle-reducer -i example.yaml -s "ceilometer"

  Reduce to only keystone and cinder, with all of the directly-related
  services, write to a new file, overwrite if it exists, with debug output on.
      ./bundle-reducer -yd -i example.yaml -o my_new.yaml -s "keystone,cinder"

  Reduce to only keystone and cinder, with none of the directly-related
  services, remove all constraints, write to a new file, with debug output on.
      ./bundle-reducer -d -i example.yaml -o my_new.yaml -s "keystone,cinder" --Xr --Xc

  Reduce to only keystone and cinder, plus any related services, and
  overwrite existing file, with debug output on.
      ./bundle-reducer -yd -i example.yaml -o example.yaml -s "keystone,cinder"



Usage: bundle-reducer [options]

Options:
  -h, --help            show this help message and exit
  -d, --debug           Enable debug logging.  (Default: False)
  -y, --yes-overwrite   Overwrite the output file.  (Default: False)
  -i IN_FILE, --in-file=IN_FILE
                        YAML input (source) file.  (Required, no default)
  -o OUT_FILE, --out-file=OUT_FILE
                        YAML output (destination) file. (Default:
                        ./out_<random>.yaml
  -s INCLUDE_SERVICES, --services=INCLUDE_SERVICES, --service=INCLUDE_SERVICES
                        Comma-separated list of Juju services to include.
                        (Default=ALL)
  -e EXCLUDE_SERVICES, --exclude=EXCLUDE_SERVICES
                        Comma-separated list of Juju services to exclude. No
                        spaces.(Default=None)
  --Xr, --exclude-related
                        Exclude related services.
  --Xc, --remove-constraints
                        Remove all constraints.
  --Xp, --remove-placements
                        Remove all placements.
```
