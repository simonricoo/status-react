{ config, stdenv, pkgs, callPackage, fetchurl, fetchFromGitHub, target-os,
  mkFilter, localMavenRepoBuilder, maven, status-go, composeXcodeWrapper, nodejs, yarn, prod-build-fn }:

with stdenv;

let
  platform = callPackage ../platform.nix { inherit target-os; };
  xcodewrapperArgs = {
    version = "10.2.1";
  };
  xcodeWrapper = composeXcodeWrapper xcodewrapperArgs;
  androidPlatform = callPackage ./android { inherit config pkgs target-os mkFilter nodejs maven localMavenRepoBuilder nodeProjectName developmentNodePackages projectNodePackage prod-build-fn; status-go = status-go.android; };
  iosPlatform = callPackage ./ios { inherit config pkgs mkFilter xcodeWrapper nodeProjectName developmentNodePackages projectNodePackage; status-go = status-go.ios; };
  fastlane = callPackage ./fastlane { inherit stdenv target-os; };
  selectedSources = [
      fastlane
      status-go.android
      status-go.ios
    ] ++
    lib.optional platform.targetAndroid androidPlatform ++
    lib.optional platform.targetIOS iosPlatform;

  yarn2nix = import (fetchFromGitHub {
    owner = "moretea";
    repo = "yarn2nix";
    rev = "3cc020e384ce2a439813adb7a0cc772a034d90bb";
    sha256 = "0h2kzdfiw43rbiiffpqq9lkhvdv8mgzz2w29pzrxgv8d39x67vr9";
    name = "yarn2nix-source";
  }) { inherit pkgs nodejs yarn; };
  developmentNodePackages = yarn2nix.mkYarnPackage {
    name = nodeProjectName;
    src = ./yarn2nix/development/.;
    packageJSON = ./yarn2nix/development/package.json;
    yarnLock = ./yarn2nix/development/yarn.lock;
    publishBinsFor = [ "genversion" ];
  };
  projectNodePackage = yarn2nix.mkYarnPackage {
    name = nodeProjectName;
    src = ../../mobile_files/.;
    packageJSON = ../../mobile_files/package.json.orig;
    yarnLock = ../../mobile_files/yarn.lock;
    # Ensure that a package.json is present where yarn2nix expects it, instead of the package.json.orig
    preConfigure = ''
      ln -s package.json.orig package.json
    '';
    postInstall = ''
      rm $out/libexec/${nodeProjectName}/node_modules/${nodeProjectName}
    '';
    publishBinsFor = [ "react-native" ];
  };
  nodeProjectName = "StatusIm";

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
    inherit prod-build fastlane;
  }
