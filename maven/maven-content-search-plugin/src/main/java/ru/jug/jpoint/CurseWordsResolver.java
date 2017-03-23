package ru.jug.jpoint;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@Mojo(name = "moderate",
        executionStrategy = "always",
        threadSafe = true,
        defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class CurseWordsResolver extends AbstractMojo {

    @Parameter(property = "words", required = true)
    String[] words;

    @Parameter(property = "directory", defaultValue = "${project.build.sourceDirectory}")
    File directory;

    @Parameter(property = "include", defaultValue = "*.java")
    String include;

    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    String encoding;

    @Parameter(property = "fatal")
    boolean fatal = false;

    private Charset getCharset() {
        if (encoding == null) {
            return Charset.defaultCharset();
        } else {
            return Charset.forName(encoding);
        }
    }

    private String report(Path file, int line, String word) {
        return file + ":" + line + " contains '" + word + "'\n";
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // so long as this is empty, the build will succeed
        final StringBuilder reports = new StringBuilder();

        try {
            final Charset charset = getCharset();
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + include);

            Files.walkFileTree(Paths.get(directory.toURI()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    FileVisitResult result = super.visitFile(file, attrs);

                    if (matcher.matches(file)) {
                        List<String> lines =  Files.readAllLines(file, charset);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            for (String word : words) {
                                if (line.contains(word)) {
                                    reports.append(report(file, i + 1, word));
                                }
                            }
                        }
                    }

                    return result;
                }
            });

        } catch (IOException e) {
            throw new MojoExecutionException("Error walking source tree", e);
        }

        if (reports.length() > 0) {
            String message = "Source code matching '" + include + "' in '" + directory +"' contains obscene " +
                    "parental guidelines violations:\n" +
                    reports + "\n" +
                    "** Moderator: Parents strongly cautioned. " +
                    "Some material may be inappropriate for children under 13. **\n";

            if (fatal) {
                getLog().error(message);

                // =expected build failure
                throw new MojoFailureException("\n ** Moderator: Parents strongly cautioned. " +
                        "Some material may be inappropriate for children under 13. **\n");
            } else {
                getLog().warn(message);
            }
        } else {
            getLog().info("No curses found");

        }

    }
}
