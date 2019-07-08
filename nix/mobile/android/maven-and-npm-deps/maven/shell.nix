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

    nodeProjectName=`ls ${projectNodePackage}/libexec`
    cp -aL --copy-contents ${projectNodePackage}/libexec/$nodeProjectName/node_modules/. ./node_modules/
    chmod -R u+w ./node_modules
    cp -aL --copy-contents ${projectNodePackage}/libexec/$nodeProjectName/deps/$nodeProjectName/node_modules/. ./node_modules/
    chmod -R u+w ./node_modules/react-native
  '';
}
