{ lib, stdenv, utils, callPackage, buildGoPackage
, go, androidPkgs, openjdk, gomobile, xcodeWrapper
# object with source attributes
, meta, source
, nimbusWrapper ? null
, platform ? "android"
, architectures ? [ "arm64" "arm" "386" ]
, goBuildFlags ? [ ]
, goBuildLdFlags ? [ ]
, outputFileName ? "status-go-${source.shortRev}-${platform}.aar" }:

let
  inherit (lib)
    concatStringsSep concatMapStrings optionalString makeBinPath optional;

  removeReferences = [ go ];
  removeExpr = refs: ''remove-references-to ${concatMapStrings (ref: " -t ${ref}") refs}'';

  # used when we want to include the Nimbus wrapper
  nimbusBridgeVendorDir = "$NIX_BUILD_TOP/go/src/${source.goPackagePath}/vendor/${source.goPackagePath}/eth-node/bridge/nimbus";

  # formatted for use with -target
  targetArchs = map (a: "${platform}/${a}") architectures;

in buildGoPackage {
  pname = source.repo;
  version = "${source.cleanVersion}-${source.shortRev}-${platform}";

  inherit meta;
  inherit (source) src goPackagePath;

  nativeBuildInputs = [ gomobile ]
    ++ optional (platform == "android") openjdk
    ++ optional stdenv.isDarwin xcodeWrapper;

  # Fixes Cgo related build failures (see https://github.com/NixOS/nixpkgs/issues/25959 )
  hardeningDisable = [ "fortify" ];

  # Ensure XCode is present, instead of failing at the end of the build
  preConfigure = optionalString stdenv.isDarwin utils.enforceXCodeAvailable;

  # OPTIONAL: Copy the Nimbus API artifacts to the expected vendor location
  nimbusPrep = optionalString (nimbusWrapper != null) (''
    cp ${nimbusWrapper}/{include/*,lib/libnimbus.a} ${nimbusBridgeVendorDir}
    chmod +w ${nimbusBridgeVendorDir}/libnimbus.{a,h}
  '');

  # Build mobile libraries
  preBuild =
    let
      NIX_GOWORKDIR = "$NIX_BUILD_TOP/go-build";
    in ''
      runHook nimbusPrep

      mkdir ${NIX_GOWORKDIR}

      export GO111MODULE=off
      export GOPATH=${gomobile.dev}:$GOPATH
      export NIX_GOWORKDIR=${NIX_GOWORKDIR}

    '' + optionalString (platform == "android") ''
      export ANDROID_HOME=${androidPkgs}
      export ANDROID_NDK_HOME=${androidPkgs}/ndk-bundle
      export PATH="${makeBinPath [ openjdk ]}:$PATH"
    '';

  # Build the Go library using gomobile for each of the configured platforms
  buildPhase = let
    ldFlags = [ "-extldflags=-Wl,--allow-multiple-definition" ] ++ goBuildLdFlags;
    CGO_LDFLAGS = concatStringsSep " " ldFlags;
  in ''
    runHook preBuild
    runHook renameImports

    echo -e "\nBuilding for targets: ${concatStringsSep "," targetArchs}\n"

    ${gomobile}/bin/gomobile bind \
      -target=${concatStringsSep "," targetArchs} \
      -ldflags="${CGO_LDFLAGS}" \
      ${optionalString (platform == "android") "-androidapi 23"} \
      ${optionalString (platform == "ios") "-iosversion=8.0"} \
      ${concatStringsSep " " goBuildFlags} \
      -o ${outputFileName} \
      ${source.goPackagePath}/mobile

    rm -rf $NIX_GOWORKDIR

    runHook postBuild
  '';

  postBuild = optionalString (nimbusWrapper != null) ''
    rm ${nimbusBridgeVendorDir}/libnimbus.{a,h}
  '';

  # replace hardcoded paths to go package in /nix/store, otherwise Nix will fail the build
  fixupPhase = ''
    find $out -type f -exec ${removeExpr removeReferences} '{}' + || true
  '';

  installPhase = ''
    mkdir -p $out
    mv ${outputFileName} $out/
  '';

  outputs = [ "out" ];
}
