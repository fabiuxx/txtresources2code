package fa.gs.utils.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

@Mojo(name = "generate", requiresProject = true, defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class TxtResources2CodeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "outputDirectory", defaultValue = "text-messages")
    private String outputDirectoryPath;

    @Parameter(property = "inputDirectory", defaultValue = "${project.build.resources[0].directory}")
    private String inputDirectoryPath;

    @Parameter(property = "packageName", defaultValue = "fa.gs.resources.text")
    private String packageName;

    private final Pattern formatWildcardPattern = Pattern.compile("\\{[^\\{\\}]*\\}");

    private final Pattern wildcardIndexPattern = Pattern.compile("(\\d+),?");

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Carpeta de salida.
        File outputDirectory = Paths.get(project.getBasedir().getAbsolutePath(), "target", "generated-sources", outputDirectoryPath).toFile();
        try {
            if (!FileUtils.fileExists(outputDirectory.getAbsolutePath())) {
                FileUtils.forceMkdir(outputDirectory);
            } else {
                FileUtils.cleanDirectory(outputDirectoryPath);
            }
        } catch (IOException ex) {
            throw new MojoFailureException("No se pudo crear la carpeta de salida.", (Throwable) ex);
        }

        // Procesar archivos de mensajes.
        final Map<String, String> msgs = new HashMap<>();
        try {
            // Listar archivos.
            List<File> files = new LinkedList<>();
            if (FileUtils.fileExists(inputDirectoryPath)) {
                File inputDirectory = new File(inputDirectoryPath);
                files = FileUtils.getFiles(inputDirectory, "**/*.msg", null);
            }
            // Extraer mensajes.
            if (files != null && !files.isEmpty()) {
                for (final File file : files) {
                    getLog().info((CharSequence) ("Procesando: " + file.getAbsolutePath()));
                    extract(msgs, file);
                }
            }
        } catch (IOException ex) {
            throw new MojoFailureException("No se pudo procesar la carpeta de entrada.", ex);
        }

        // Generar mensajes en forma de código fuente.
        try {
            dump(msgs, outputDirectory.getAbsolutePath());
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        } catch (IOException ex) {
            throw new MojoFailureException("No se pudo generar el código fuente de salida.", ex);
        }
    }

    private void extract(final Map<String, String> msgs, final File file) throws IOException {
        final Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.trim();
            if (!line.isEmpty()) {
                final String[] parts = line.split("=");
                final String name = parts[0].trim();
                final String value = parts[1].trim();
                msgs.put(name, value);
            }
        }
    }

    private void dump(final Map<String, String> msgs, String path) throws IOException {
        final String className = "TextMessages";
        final File target = Paths.get(path, className + ".java").toFile();
        FileUtils.fileWrite(target, "");
        FileUtils.fileAppend(target.getAbsolutePath(), String.format("package %s;\n", packageName));
        FileUtils.fileAppend(target.getAbsolutePath(), String.format("import java.text.MessageFormat;\n", new Object[0]));
        FileUtils.fileAppend(target.getAbsolutePath(), String.format("public class %s { \n", className));
        for (final Map.Entry<String, String> entry : msgs.entrySet()) {
            final String valName = entry.getKey().toUpperCase().replaceAll(" ", "_");
            FileUtils.fileAppend(target.getAbsolutePath(), String.format("public static final String %s = \"%s\"; \n", valName, entry.getValue()));
            FileUtils.fileAppend(target.getAbsolutePath(), String.format("public static final String %s%s\n\n", valName, genFormatterFnBody(valName, entry.getValue())));
        }
        FileUtils.fileAppend(target.getAbsolutePath(), String.format("}", new Object[0]));
    }

    private String genFormatterFnBody(final String name, final String value) {
        final Matcher matcher = formatWildcardPattern.matcher(value);
        Integer maxIndex = -1;
        while (matcher.find()) {
            final Matcher matcher2 = wildcardIndexPattern.matcher(matcher.group(0));
            if (matcher2.find()) {
                final Integer index = Integer.valueOf(matcher2.group(1));
                maxIndex = Math.max(index, maxIndex);
            }
        }
        final int argsc = maxIndex + 1;
        final StringBuilder builder = new StringBuilder();
        if (argsc == 0) {
            builder.append(String.format("() { return %s; }", name));
        } else {
            builder.append(String.format("(%s) { return MessageFormat.format(%s, %s); }", genArgsList(argsc, "Object "), name, genArgsList(argsc, "")));
        }
        return builder.toString();
    }

    private String genArgsList(final int argsc, final String prefix) {
        final StringBuilder builder = new StringBuilder();
        if (argsc == 1) {
            builder.append(String.format("%sa0", prefix));
        } else if (argsc > 1) {
            for (int i = 0; i < argsc - 1; ++i) {
                builder.append(String.format("%sa%d,", prefix, i));
            }
            builder.append(String.format("%sa%d", prefix, argsc - 1));
        }
        return builder.toString();
    }
}
