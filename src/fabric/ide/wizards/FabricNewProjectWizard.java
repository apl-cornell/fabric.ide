package fabric.ide.wizards;

import fabric.ide.FabricPluginInfo;
import jif.ide.wizards.JifNewProjectWizard;
import polyglot.ide.PluginInfo;

public class FabricNewProjectWizard extends JifNewProjectWizard {

  public FabricNewProjectWizard() {
    this(FabricPluginInfo.INSTANCE);
  }

  public FabricNewProjectWizard(PluginInfo pluginInfo) {
    super(pluginInfo);
  }

  @Override
  public void addExtraPages() {
    pageTwo = new FabricNewProjectWizardPageTwo(pluginInfo,
        "new" + pluginInfo.langShortName() + "ProjectPageTwo");
    addPage(pageTwo);
  }

}
