package fi.jyu.jenkins.pharo;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher.ProcStarter;
import hudson.Messages;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link Builder} to run Pharo/Squeak images.
 *
 * <p>
 * {@code RunPharoImage} runs a script using given
 * Squeak/Pharo virtual machine and image.
 * It assumes that virtual machine takes two parameters
 * image and the script file to be run.
 *
 * @author Panu Suominen <panu.suominen@iki.fi>
 */
public class RunPharoImage extends Builder {

    private final String virtualMachineName;
    private final String startImageName;
    private final String executeCode;
    private final String resultingImageName;
    private final String parameters;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RunPharoImage(String virtualMachineName, String startImageName, String executeCode, String resultingImageName, String parameters) {
        this.virtualMachineName = virtualMachineName;
        this.startImageName = startImageName;
        this.executeCode = executeCode;
        this.resultingImageName = resultingImageName;
        this.parameters = parameters;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public String getResultingImageName() {
        return resultingImageName;
    }

    public String getStartImageName() {
        return startImageName;
    }

    public String getParameters() {
        if (null == parameters) {
            return "";
        } else {
            return parameters;
        }
    }

    public String getActualStartImageName() {
        if (null == getStartImageName() || getStartImageName().trim().isEmpty()) {
            return getSqueakVM().getDefaultImageName();
        }
        return getStartImageName();
    }

    public String getExecuteCode() {
        return executeCode;
    }

    public boolean usesDefaultImage() {
        return (getStartImageName().equals(getSqueakVM().getDefaultImageName()));
    }

    public SqueakVM getSqueakVM() {
        for (SqueakVM i : getDescriptor().getInstallations()) {
            if (virtualMachineName != null && virtualMachineName.equals(i.getName())) {
                return i;
            }
        }
        return null;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        PrintStream out = listener.getLogger();
        out.println("Running Pharo/Squeak image");
        String vmExcutable = getSqueakVM().getExcutableLocation();
        ProcStarter proc = launcher.launch();
        FilePath codeFile = build.getWorkspace().createTempFile("builder", ".st");
        FilePath tmpImageFile = new FilePath(build.getWorkspace(), "temp.image");
        FilePath tmpChangesFile = new FilePath(build.getWorkspace(), "temp.changes");
        FilePath imageFile = new FilePath(build.getWorkspace(), getActualStartImageName() + ".image");
        FilePath changesFile = new FilePath(build.getWorkspace(), getActualStartImageName() + ".changes");
        FilePath resultingImageFile = new FilePath(build.getWorkspace(), getResultingImageName() + ".image");
        FilePath resultingChangesFile = new FilePath(build.getWorkspace(), getResultingImageName() + ".changes");


        // Copy image to temp image used to do the build.
        imageFile.copyTo(tmpImageFile);
        changesFile.copyTo(tmpChangesFile);

        // Build arguments.
        ArgumentListBuilder argumentListBuilder = new ArgumentListBuilder();
        argumentListBuilder.add(vmExcutable);
        if (null != getParameters() && !getParameters().trim().isEmpty()){
            argumentListBuilder.add(getParameters());
        }
        argumentListBuilder.add(tmpImageFile);
        argumentListBuilder.add(codeFile);

        try {
            PrintStream codeOut = new PrintStream(codeFile.write(), false, "utf-8");
            codeOut.println(getSqueakVM().getBeforeBlock());
            codeOut.println(getExecuteCode());
            codeOut.println(getSqueakVM().getAfterBlock());
            codeOut.close();

            proc.pwd(build.getWorkspace());
            proc.cmds(argumentListBuilder);
            proc.stdout(out);
            proc.stderr(out);
            proc.join();
            tmpImageFile.renameTo(resultingImageFile);
            tmpChangesFile.renameTo(resultingChangesFile);
            out.println("Renamed image to " + resultingImageFile.getName());
        } finally {
            codeFile.delete();
            tmpImageFile.delete();
            tmpChangesFile.delete();
        }
        listener.getLogger().println("Pharo/Squeak image returned");
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getDisplayName() {
            return "Run Squeak/Pharo image";
        }

        public SqueakVM[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(SqueakVM.DescriptorImpl.class).getInstallations();
        }

        public FormValidation doCheckImage(@QueryParameter String imageName) {
            File imageFile = new File(imageName + ".image");
            File changesFile = new File(imageName + ".changes");

            if (!imageFile.exists()) {
                return FormValidation.error(Messages.FilePath_validateRelativePath_noSuchFile(changesFile));
            }

            if (!changesFile.exists()) {
                return FormValidation.error(Messages.FilePath_validateRelativePath_noSuchFile(changesFile));
            }

            return FormValidation.ok();
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(RunPharoImage.class, formData);
        }
    }
}
