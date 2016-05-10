/*
 * File: Generator.java
 * Package: net.softhome.eric.tools.generator
 * Project: Xsd2SaxParser
 * Created on: 02 Dec 2009
 * By: Eric Blanchard
 *
 */
package net.softhome.eric.tools.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.softhome.eric.tools.parser.XSDParser;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Generator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    public Generator() throws Exception {
        Properties props = new Properties();
        props.setProperty("resource.loader", "class");
        props.setProperty("class.resource.loader.description",
                "Velocity Classpath Resource Loader");
        props.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        props.setProperty("class.resource.loader.path", ".");
        Velocity.init(props);

    }

    public void execute(String[] schemaFiles, final String outputDirectory,
            final String packageName, final String className,
            final boolean useSlf4j, final String template) throws Exception {
        Template mainTemplate = Velocity.getTemplate(template);

        Map<String, String> elements = new HashMap<String, String>();
        Map<String, String> attributes = new HashMap<String, String>();
        XSDParser xsdp = XSDParser.getInstance();
        StringBuilder schemaFilesStr = null;
        for (String schemaFile : schemaFiles) {
            FileInputStream fis = new FileInputStream(schemaFile);
            xsdp.reset();
            xsdp.parse(fis);
            fis.close();
            elements.putAll(xsdp.getElements());
            attributes.putAll(xsdp.getAttributes());
            if (schemaFilesStr == null) {
                schemaFilesStr = new StringBuilder(schemaFile);
            } else {
                schemaFilesStr.append(", ");
                schemaFilesStr.append(schemaFile);
            }

        }

        VelocityContext context = new VelocityContext();
        LOGGER.debug("execute: Got {} elements, and {} attributes", elements.size(), attributes.size());
        context.put("elements", elements);
        context.put("attributes", attributes);
        context.put("package", packageName);
        context.put("class", className);
        context.put("schema", schemaFilesStr.toString());
        context.put("date", new Date().toString());
        context.put("useSlf4j", useSlf4j);

        String dir = outputDirectory + "/" + packageName.replace('.', '/')
                + "/";
        LOGGER.debug("Generating to package {} to dirctory {}", packageName,
                dir);

        File f = new File(dir);
        f.mkdirs();

        FileWriter fw = new FileWriter(dir + className + ".java");
        mainTemplate.merge(context, fw);
        fw.flush();
        fw.close();

    }

    public static void main(String args[]) throws Exception {
        CommandLineParser clp = new PosixParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine commandLine = null;
        String outputDir;
        String packageName;
        String className;
        String template;
        boolean verbose = false;
        boolean useSlf4j = true;

        // create the Options
        Options options = new Options();
        options.addOption("v", "verbose", false, "display verbose traces");
        options.addOption(OptionBuilder.withLongOpt("package")
                .withDescription("package name for generated parser")
                .hasArgs(1)
                .withArgName("package-name")
                .isRequired()
                .create('p'));
        options.addOption(OptionBuilder.withLongOpt("outout-dir")
                .withDescription("output directory for the generated parser source")
                .hasArgs(1)
                .withArgName("directory")
                .create('o') );
        options.addOption(OptionBuilder.withLongOpt("parser-class-name")
                .withDescription("class name for the generated parser")
                .hasArgs(1)
                .withArgName("class-name")
                .create('c'));
        options.addOption(OptionBuilder.withLongOpt("template-file")
                .withDescription("alternate template file for SAX parser")
                .hasArgs(1)
                .withArgName("template-file")
                .create('t'));
        options.addOption("n", "no-slf4j", false, "do not use slf4j logging in parser");

        try {
            // parse the command line arguments
            commandLine = clp.parse(options, args);

            outputDir = commandLine.getOptionValue("o", "src/main/java");
            packageName = commandLine.getOptionValue("p", "org.company.test.parser");
            className = commandLine.getOptionValue("c", "SaxParser");
            verbose = commandLine.hasOption("v");
            useSlf4j = ! commandLine.hasOption("n");
            template = commandLine.getOptionValue("t", "templates/parser_skel.vm");
        }
        catch( ParseException exp ) {
            LOGGER.error("main: Unexpected exception: {}", exp.getMessage(), exp);
            formatter.printHelp("generate [options] <XSD file> ... ", options);
            return;
        }

        LOGGER.debug("main: outputDir={}, packageName={}",
                outputDir, packageName);
        LOGGER.debug("main: className={}, verbose={}",
                className, verbose);

        String[] leftArgs = commandLine.getArgs();

        LOGGER.debug("main: letfArgs={}", leftArgs);
        if (leftArgs == null || leftArgs.length == 0) {
            formatter.printHelp("generate [options] <XSD file> ... ", options);
            return;
        }


        Generator g = new Generator();
        g.execute(leftArgs, outputDir, packageName, className, useSlf4j,
                template);
    }
}
