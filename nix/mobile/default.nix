{ config, stdenv, pkgs, callPackage, fetchurl, target-os,
  mkFilter, localMavenRepoBuilder, maven, status-go, composeXcodeWrapper, nodejs, prod-build-fn }:

with stdenv;

let
  platform = callPackage ../platform.nix { inherit target-os; };
  xcodewrapperArgs = {
    version = "10.2.1";
  };
  xcodeWrapper = composeXcodeWrapper xcodewrapperArgs;
  androidPlatform = callPackage ./android { inherit config pkgs target-os mkFilter nodejs maven localMavenRepoBuilder nodeProjectName developmentNodePackages projectNodePackage prod-build-fn; status-go = status-go.android; };
  iosPlatform = callPackage ./ios { inherit config pkgs mkFilter xcodeWrapper nodeProjectName developmentNodePackages projectNodePackage; status-go = status-go.ios; };
  selectedSources = [
      status-go.android
      status-go.ios
    ] ++
    lib.optional platform.targetAndroid androidPlatform ++
    lib.optional platform.targetIOS iosPlatform;

  developmentNodePackages = import ./node2nix/development { inherit pkgs nodejs; };
  projectNodePackage' = import ./node2nix/StatusIm { inherit pkgs nodejs; };
  projectNodePackage = projectNodePackage'.package.override(oldAttrs: (realmOverrides oldAttrs) // {
    # Ensure that a package.json is present where node2nix's node-env.nix expects it, instead of the package.json.orig
    postPatch = ''
      outputPackage="$out/lib/node_modules/${nodeProjectName}/package.json"
      mkdir -p $(dirname $outputPackage)
      cp $src/package.json.orig $outputPackage
      chmod +w $outputPackage
      unset outputPackage
    '';
  });
  nodeProjectName = "StatusIm";
  realmOverrides = import ./realm-overrides { inherit stdenv target-os nodeProjectName fetchurl nodejs; };

  # TARGETS
  prod-build = prod-build-fn { inherit projectNodePackage; };

in
  {
    buildInputs = lib.catAttrs "buildInputs" selectedSources;
    shellHook = lib.concatStrings (lib.catAttrs "shellHook" selectedSources);

    # CHILD DERIVATIONS
    android = androidPlatform;
    ios = iosPlatform;

    # TARGETS
    inherit prod-build;
  }
