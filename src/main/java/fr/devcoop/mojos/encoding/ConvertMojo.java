package fr.devcoop.mojos.encoding;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.io.ByteStreams;
import com.google.common.io.OutputSupplier;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.DirectoryWalker;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Goal which convert all sources and resources from an encoding to another one.
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
    private List<Resource> resources;
    @Parameter(defaultValue = "${project.testResources}")
    private List<Resource> testResources;
    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            getLog().info(String.format("Convert sources and resources from %s to %s", sourceEncoding, targetEncoding));
            List<String> roots = new ArrayList();
            roots.addAll(sourceRoots);
            roots.addAll(testSourceRoots);
            roots.addAll(Collections2.transform(resources, new Function<Resource, String>() {
                public String apply(Resource input) {
                    return input.getDirectory();
                }
            }));
            roots.addAll(Collections2.transform(testResources, new Function<Resource, String>() {
                public String apply(Resource input) {
                    return input.getDirectory();
                }
            }));
        
            for (ExtraSupportedPackaging supportedPackaging : ExtraSupportedPackaging.values()) {
                roots.addAll(supportedPackaging.getSrcDirExtractor().apply(project));
            }
            
            for (String sourceDirectory : roots) {
                getLog().info(String.format("Converting %s", sourceDirectory));
                MyDirectoryWalker walker = new MyDirectoryWalker();
                walker.walk(new File(sourceDirectory));
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private class MyDirectoryWalker extends DirectoryWalker {

        public MyDirectoryWalker() {
            super(new FileFilter() {
                public boolean accept(File pathname) {
                    try {
                        getLog().debug(pathname.getCanonicalPath());
                        // TODO : remove this ugly filtering
                        return pathname.isDirectory() || pathname.getCanonicalPath().endsWith(".java")
                                || pathname.getCanonicalPath().endsWith(".properties")
                                || pathname.getCanonicalPath().endsWith(".html")
                                || pathname.getCanonicalPath().endsWith(".xhtml")
                                || pathname.getCanonicalPath().endsWith(".jsp");
                    } catch (IOException ex) {
                        return false;
                    }
                }
            }, -1);
        }

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


 private enum ExtraSupportedPackaging {

        WAR("war", SrcWebappDirExtractor.INSTANCE);
        private String name;
        private Function<MavenProject, List<String>> srcDirExtractor;

        private ExtraSupportedPackaging(String name, Function<MavenProject, List<String>> srcDirExtractor) {
            this.name = name;
            this.srcDirExtractor = srcDirExtractor;
        }

        public String getName() {
            return name;
        }

        public Function<MavenProject, List<String>> getSrcDirExtractor() {
            return srcDirExtractor;
        }
    }

    private enum SrcWebappDirExtractor implements Function<MavenProject, List<String>> {

        INSTANCE;

        public List<String> apply(MavenProject input) {
            if (!"war".equals(input.getPackaging())) {
                return Collections.emptyList();
            }
            Plugin warPlugin = input.getPlugin("org.apache.maven.plugins:maven-war-plugin");

            File warSources = new File(input.getBasedir(), "/src/main/webapp");
            if (warPlugin.getConfiguration() != null) {
                Xpp3Dom dom = (Xpp3Dom) warPlugin.getConfiguration();
                Xpp3Dom warSourceDirectory = dom.getChild("warSourceDirectory");
                if (warSourceDirectory != null) {
                    warSources = new File(input.getBasedir(), warSourceDirectory.getValue());
                }
            }
            return Collections.singletonList(warSources.toString());
        }
    }
}
