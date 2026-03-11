package org.metabit.platform.support.config.tool;

import picocli.CommandLine.Option;

public class CommonOptions
{
    @Option(names = {"-c", "--company"}, description = "Company name", defaultValue = "${env:MCONFIG_COMPANY}")
    public String company;

    @Option(names = {"-a", "--app"}, description = "Application name", defaultValue = "${env:MCONFIG_APP}")
    public String application;

    @Option(names = {"-C", "--config"}, description = "Configuration name", defaultValue = "${env:MCONFIG_CONFIG}")
    public String config;

    @Option(names = {"-K", "--key"}, description = "Key name")
    public String key;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    public boolean verbose;

    @Option(names = {"--debug"}, description = "Enable debug logging for the CLI and underlying library")
    public boolean debug;

    @Option(names = {"-f", "--format"}, description = "Output format: HUMAN, JSON, CSV, YAML, TOML", defaultValue = "HUMAN")
    public Main.OutputFormat format;

    @Option(names = {"-l", "--lang"}, description = "Preferred language for descriptions", defaultValue = "en")
    public String language;

    @Option(names = {"-X", "--experimental"}, description = "Enable experimental features")
    public boolean experimental;

    @Option(names = {"-E", "--show-events"}, description = "Show configuration events (e.g. parse warnings)")
    public boolean showEvents;

    @Option(names = {"-W", "--whitesmiths"}, description = "Use Moderate Whitesmiths indentation for JSON/YAML/TOML output")
    public boolean whitesmiths;
}
