#!/bin/sh

# File: generate.sh
# Project: Xsd2SaxParser
# Created on: 02 Dec 2009
# By: Eric Blanchard

mainClass=net.softhome.eric.tools.generator.Generator

echo "java -cp .:lib $mainClass $*"
java -cp .:lib/* $mainClass $*
