{ stdenv, callPackage,
  qt5, status-go }:

with stdenv;

let
  baseImage = callPackage ./base-image { };

in
{
  buildInputs = [
    baseImage status-go.buildInputs
    qt5.full
  ];

  shellHook =
    baseImage.shellHook +
    status-go.shellHook + ''
      export QT_PATH="${qt5.full}"
      export QT_BASEBIN_PATH="${qt5.qtbase.bin}"
      export PATH="${qt5.full}/bin:$PATH"
    '';
}
