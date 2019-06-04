{ mkShell, curl, git, gradle, maven,
  projectNodePackage, androidEnvShellHook, status-go }:

mkShell {
  buildInputs = [
    curl
    git
    gradle
    maven
    projectNodePackage
  ];
  shellHook =
    androidEnvShellHook +
    status-go.shellHook +
  ''
    if [ -d ./node_modules ]; then
      chmod -R u+w ./node_modules
      rm -rf ./node_modules
    fi

    cp -R ${projectNodePackage}/lib/node_modules/`ls ${projectNodePackage}/lib/node_modules`/node_modules . || exit
    chmod -R u+w ./node_modules/react-native
  '';
}
