@echo off

rem # File: generate.bat
rem # Project: Xsd2SaxParser
rem # Created on: 02 Dec 2009
rem # By: Eric Blanchard

set libs=lib/Xsd2SaxParser-0.0.1.jar;lib/velocity-1.6.2.jar;lib/commons-collections-3.2.1.jar;lib/commons-lang-2.4.jar;lib/slf4j-api-1.5.8.jar;lib/logback-classic-0.9.17.jar;lib/logback-core-0.9.17.jar;lib/commons-cli-1.2.jar
set mainClass=net.softhome.eric.tools.generator.Generator

%JAVA_HOME%\bin\java -classpath .;%libs% %mainClass% %*