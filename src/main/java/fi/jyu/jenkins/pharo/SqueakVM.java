package fi.jyu.jenkins.pharo;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.Messages;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.FormValidation;
import java.io.File;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import org.kohsuke.stapler.QueryParameter;

/**
 * {@link ToolInstallation} for Pharo/Squeak virtual machine.
 *
 * <p>
 * Currently inherited {@code home} field is used to store excutable
 * location.
 *
 * <p>
 * Auto installation is also missing.
 *
 * @author Panu Suominen <panu.suominen@iki.fi>
 */
public class SqueakVM extends ToolInstallation {

    private final String defaultImageName;

    public String getDefaultImageName() {
        return defaultImageName;
    }

    @DataBoundConstructor
    public SqueakVM(String name, String excutableLocation, String defaultImageName, List<? extends ToolProperty<?>> properties) {
        super(name, excutableLocation, properties);
        this.defaultImageName = defaultImageName;

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getExcutableLocation() {
        return getHome();
    }

    public String getBeforeBlock() {
        BeforeBlock beforeBlock = getProperties().get(BeforeBlock.class);
        if (null == beforeBlock) {
            return "";
        } else {
            return beforeBlock.getCode();
        }
    }

    public String getAfterBlock() {
        AfterBlock afterBlock = getProperties().get(AfterBlock.class);
        if (null == afterBlock) {
            return "";
        } else {
            return afterBlock.getCode();
        }
    }

    /**
     * Code block that is run before the build specific script.
     */
    public static class BeforeBlock extends ToolProperty<SqueakVM> {

        private final String code;

        public String getCode() {
            return code;
        }

        public BeforeBlock(String code) {
            this.code = code;
        }

        @Override
        public Class<SqueakVM> type() {
            return SqueakVM.class;
        }

        @Override
        public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
        }

        @Extension
        public static class DescriptorImpl extends ToolPropertyDescriptor {

            @Override
            public String getDisplayName() {
                return "Before block";
            }
        }
    }

    /**
     * Code block that is executed after build specific script.
     */
    public static class AfterBlock extends ToolProperty<SqueakVM> {

        private final String code;

        public String getCode() {
            return code;
        }

        public AfterBlock(String code) {
            this.code = code;
        }

        @Override
        public Class<SqueakVM> type() {
            return SqueakVM.class;
        }

        @Override
        public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
        }

        @Extension
        public static class DescriptorImpl extends ToolPropertyDescriptor {

            @Override
            public String getDisplayName() {
                return "After block";
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<SqueakVM> {

        @CopyOnWrite
        private SqueakVM[] installations = new SqueakVM[0];

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Squeak/Pharo VM";
        }

        @Override
        public SqueakVM[] getInstallations() {
            return installations;
        }

        @Override
        public void setInstallations(SqueakVM[] squeakVMs) {
            installations = squeakVMs;
            save();
        }

        public FormValidation doCheckName(@QueryParameter String name) {
            return FormValidation.validateRequired(name);
        }

        public FormValidation doCheckDefaultImageName(@QueryParameter String defaultImageName) {
            File defaultImage = new File(defaultImageName + ".image");
            File defaultChanges = new File(defaultImageName + ".changes");

            if (!defaultImage.isFile()) {
                return FormValidation.error(Messages.FilePath_validateRelativePath_noSuchFile(defaultImage.getPath()));
            }
            if (!defaultChanges.isFile()) {
                return FormValidation.error(Messages.FilePath_validateRelativePath_noSuchFile(defaultChanges.getPath()));
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckExcutableLocation(@QueryParameter File excutableLocation) {
            if (!excutableLocation.exists()) {
                return FormValidation.error("File does not exists");
            }
            if (!excutableLocation.isFile()) {
                return FormValidation.error("Not a file");
            }

            return FormValidation.ok();
        }
    }
}
