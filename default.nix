{ pkgs ? import <nixpkgs> {}, maven ? pkgs.maven, jdk ? pkgs.jdk8 }:
let
  deps = import ./deps.nix {
    inherit pkgs; 
  };
in
pkgs.stdenv.mkDerivation {
  name = "pdftools";
  src = ./.;

  buildInputs = [ maven ];
  propagatedBuildInputs = [ jdk8 ];

  phases = [ "unpackPhase" "buildPhase" "installPhase" ];

  buildPhase = ''
    ${maven}/bin/mvn verify
  '';

  installPhase = ''
    cp -av target/* $out
  '';
}
