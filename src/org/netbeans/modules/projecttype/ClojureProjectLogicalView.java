package org.netbeans.modules.projecttype;

import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.ExternalProcessBuilder;
import static org.netbeans.modules.projecttype.ClojureProject.ClojureProjectInformation.CUSTOMER_ICON;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public class ClojureProjectLogicalView implements LogicalViewProvider {

    private final ClojureProject project;

    public ClojureProjectLogicalView(ClojureProject project) {
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        try {
            //Obtain the project directory's node:
            FileObject projectDirectory = project.getProjectDirectory();
            DataFolder projectFolder = DataFolder.findFolder(projectDirectory);
            Node nodeOfProjectFolder = projectFolder.getNodeDelegate();
            //Decorate the project directory's node:
            return new ProjectNode(nodeOfProjectFolder, project);
        } catch (DataObjectNotFoundException donfe) {
            Exceptions.printStackTrace(donfe);
            //Fallback-the directory couldn't be created -
            //read-only filesystem or something evil happened
            return new AbstractNode(Children.LEAF);
        }
    }

    private final class ProjectNode extends FilterNode {

        final ClojureProject project;

        public ProjectNode(Node node, ClojureProject project)
                throws DataObjectNotFoundException {
            super(node,
                    new FilterNode.Children(node),
                    new ProxyLookup(
                            new Lookup[]{
                                Lookups.singleton(project),
                                node.getLookup()
                            }));
            this.project = project;
        }

        @Override
        public Action[] getActions(boolean arg0) {
            return new Action[]{
                new ProjectAction(ActionProvider.COMMAND_RUN, "Run", project),
                null,
                new ProjectAction(ActionProvider.COMMAND_BUILD, "JAR", project),
                new ProjectAction(ActionProvider.COMMAND_REBUILD, "Uber JAR", project),
                null,
                new ProjectAction(ActionProvider.COMMAND_CLEAN, "Clean", project),
                null,
                CommonProjectActions.closeProjectAction(),
                null,
                new ProjectAction(ActionProvider.COMMAND_PROFILE, "Help", project),
            };
        }

        @Override
        public Image getIcon(int type) {
            return ImageUtilities.loadImage(CUSTOMER_ICON);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getDisplayName() {
            return project.getProjectDirectory().getName();
        }
        
    }

    private static class ProjectAction extends AbstractAction {

        private final ClojureProject project;
        private final String command;

        public ProjectAction(String cmd, String displayName, ClojureProject prj) {
            this.project = prj;
            putValue(NAME, displayName);
            this.command = cmd;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (command.equals(ActionProvider.COMMAND_RUN)) {
                processCommand("run");
            } else if (command.equals(ActionProvider.COMMAND_CLEAN)) {
                processCommand("clean");
            } else if (command.equals(ActionProvider.COMMAND_BUILD)) {
                processCommand("jar");
            } else if (command.equals(ActionProvider.COMMAND_REBUILD)) {
                processCommand("uberjar");
            } else if (command.equals(ActionProvider.COMMAND_PROFILE)) {
                processCommand("help");
            }
        }

        private void processCommand(String command) {
            ExternalProcessBuilder processBuilder = new ExternalProcessBuilder("/usr/local/bin/lein").
                    addArgument(command).
                    workingDirectory(FileUtil.toFile(project.getProjectDirectory()));
            ExecutionDescriptor descriptor = new ExecutionDescriptor().
                    frontWindow(true).
                    showProgress(true).
                    controllable(true);
            ExecutionService service = ExecutionService.newService(
                    processBuilder,
                    descriptor,
                    command);
            service.run();
        }

        @Override
        public boolean isEnabled() {
            ActionProvider prov = (ActionProvider) project.getLookup().lookup(ActionProvider.class);
            return prov.isActionEnabled(command, null);
        }

    }

    @Override
    public Node findPath(Node node, Object o) {
        return null;
    }

}
