{
  pkgs ? import <nixpkgs> { },
}:
let
  java = pkgs.javaPackages.compiler.temurin-bin.jdk-25; # in sync with backend/Dockerfile
in
pkgs.mkShell {
  packages = [
    java
    (pkgs.maven.override {
      jdk_headless = java;
    })

    pkgs.nodejs
    pkgs.pnpm

    pkgs.postgresql_18 # for psql
    pkgs.clang-tools # for clang-format
  ];
}
