{
  pkgs ? import <nixpkgs> { },
}:
let
  java = pkgs.openjdk25_headless;
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
