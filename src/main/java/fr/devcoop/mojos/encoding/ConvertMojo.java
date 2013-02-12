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

/**
 * Goal which convert all sources and resources from an encoding to another one.
 *
 * @goal convert
 *
 * @phase process-sources
 */
public class ConvertMojo extends AbstractMojo {

    /**
     * @parameter @required @readonly
     */
    private String sourceEncoding;
    /**
     * @parameter @required @readonly
     */
    private String targetEncoding;
    /**
     * @parameter default-value="${project.compileSourceRoots}"
     * @required
     * @readonly
     */
    private List<String> sourceRoots;
    /**
     * @parameter default-value="${project.testSourceRoots}"
     * @required
     * @readonly
     */
    private List<String> testSourceRoots;
    /**
     * @parameter default-value="${project.resources}"
     * @required
     * @readonly
     */
    private List<String> resources;
    /**
     * @parameter default-value="${project.resources}"
     * @required
     * @readonly
     */
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
