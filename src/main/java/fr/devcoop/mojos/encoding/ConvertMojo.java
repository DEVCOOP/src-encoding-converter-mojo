package fr.devcoop.mojos.encoding;

import com.google.common.io.ByteStreams;
import com.google.common.io.OutputSupplier;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.DirectoryWalker;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which convert all sources and resources from an encoding to another one.
 *
 *
 */
@Mojo(name = "convert")
public class ConvertMojo extends AbstractMojo {

    @Parameter(defaultValue = "${sourceEncoding}")
    private String sourceEncoding;
    @Parameter(defaultValue = "${targetEncoding}")
    private String targetEncoding;
    @Parameter(defaultValue = "${project.compileSourceRoots}")
    private List<String> sourceRoots;
    @Parameter(defaultValue = "${project.testSourceRoots}")
    private List<String> testSourceRoots;
    @Parameter(defaultValue = "${project.resources}")
    private List<String> resources;
    @Parameter(defaultValue = "project.testResources")
    private List<String> testResources;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            List<String> roots = new ArrayList(resources);
            roots.addAll(sourceRoots);
            roots.addAll(testSourceRoots);
            roots.addAll(testResources);
            for (String sourceDirectory : sourceRoots) {
                MyDirectoryWalker walker = new MyDirectoryWalker();
                walker.walk(new File(sourceDirectory));
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private class MyDirectoryWalker extends DirectoryWalker {

        @Override
        protected void handleFile(final File file, int depth, Collection results) throws IOException {
            byte[] content = new byte[(int) file.length()];
            ByteStreams.readFully(new FileInputStream(file), content);
            String srcContent = new String(content, sourceEncoding);
            ByteStreams.write(srcContent.getBytes(targetEncoding), new OutputSupplier<OutputStream>() {
                public OutputStream getOutput() throws IOException {
                    return new FileOutputStream(file);
                }
            });
        }

        public void walk(File file) throws IOException {
            super.walk(file, new ArrayList());
        }
    }
}
