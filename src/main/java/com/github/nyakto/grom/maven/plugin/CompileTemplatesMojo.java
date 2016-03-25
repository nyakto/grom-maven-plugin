package com.github.nyakto.grom.maven.plugin;

import com.github.nyakto.grom.GromOptions;
import com.github.nyakto.grom.TemplateSourceCode;
import com.github.nyakto.grom.ast.TemplateNode;
import com.github.nyakto.grom.lexer.GromLexer;
import com.github.nyakto.grom.parser.GromParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.*;
import java.util.List;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CompileTemplatesMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDirectory;
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/grom")
    private String generatedSourcesDir;
    @Parameter
    private List<String> includes;
    @Parameter
    private List<String> excludes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sourceDirectory);
        if (includes != null) {
            scanner.setIncludes(includes.toArray(new String[includes.size()]));
        }
        if (excludes != null) {
            scanner.setIncludes(excludes.toArray(new String[excludes.size()]));
        }
        scanner.scan();
        for (String fileName : scanner.getIncludedFiles()) {
            compileTemplate(fileName);
        }
    }

    protected void compileTemplate(String fileName) throws MojoExecutionException {
        try {
            final File sourceFile = new File(sourceDirectory, fileName);
            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile))) {
                try (Reader reader = new InputStreamReader(inputStream)) {
                    final GromLexer lexer = new GromLexer(reader);
                    final GromParser parser = new GromParser(lexer);
                    final TemplateNode template = parser.parseTemplate();
                    final TemplateSourceCode compilationResult = template.compile(
                        GromOptions.builder()
                            .build()
                    );
                    final File targetFile = new File(
                        generatedSourcesDir,
                        classNameToFileName(compilationResult.getClassName())
                    );
                    FileUtils.forceMkdir(targetFile.getParentFile());
                    FileUtils.writeStringToFile(
                        targetFile,
                        compilationResult.getSourceCode(),
                        "UTF-8"
                    );
                }
            }
        } catch (Exception error) {
            throw new MojoExecutionException(
                String.format(
                    "failed to generate groovy sources for GROM template %s",
                    fileName
                ),
                error
            );
        }
    }

    protected String classNameToFileName(String className) {
        return StringUtils.deleteWhitespace(className.replace('.', File.separatorChar)) + ".groovy";
    }
}
