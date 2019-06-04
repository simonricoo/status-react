# npm dependencies Nix wrapper

## Overview

This folder contains two node2nix-generated folders that wrap the npm installation. It consists of:

- `generate-nix.sh`: This is the main entry point script, which will use node2nix to determine the build-time and run-time node dependencies and output them to the `development` and `StatusIm` directories.
- `development`: This directory contains the node packages that are used in a development environment (hardcoded in `generate-nix.sh`).
- `StatusIm`: This directory contains the node packages that are specified in `mobile_files/package.json.orig`.
