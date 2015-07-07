package fabric.ide.wizards;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import fabric.ide.FabricPlugin;
import jif.ide.wizards.JifNewProjectWizardPageTwo;
import polyglot.ide.PluginInfo;
import polyglot.ide.common.BuildpathEntry;
import polyglot.ide.wizards.LibrarySelector;

public class FabricNewProjectWizardPageTwo extends JifNewProjectWizardPageTwo {
  protected LibrarySelector fabilSigpathSelector;

  public FabricNewProjectWizardPageTwo(PluginInfo pluginInfo, String name) {
    this(pluginInfo, name, null);
  }

  public FabricNewProjectWizardPageTwo(PluginInfo pluginInfo, String name,
      IProject project) {
    super(pluginInfo, name, project);
  }

  @Override
  public void addExtraBuildPathTabs(TabFolder tabFolder) {
    super.addExtraBuildPathTabs(tabFolder);

    fabilSigpathSelector = new LibrarySelector(tabFolder);
    if (project != null) {
      fabilSigpathSelector
          .setItems(getBuildpathResources(FabricPlugin.FABIL_SIGPATH));
    }

    TabItem item3 = new TabItem(tabFolder, SWT.NONE);
    item3.setText("&FabIL sigpath");
    item3.setControl(fabilSigpathSelector);
  }

  @Override
  public List<BuildpathEntry> getBuildpathEntries() {
    List<BuildpathEntry> result = super.getBuildpathEntries();
    return addBuildpathEntries(FabricPlugin.FABIL_SIGPATH, BuildpathEntry.LIB,
        fabilSigpathSelector.getItems(), result);
  }

}
