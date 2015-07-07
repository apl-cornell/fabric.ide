package fabric.ide.wizards;

import fabric.ide.FabricPluginInfo;
import jif.ide.wizards.JifConfigureBuildPathCommandHandler;
import polyglot.ide.PluginInfo;

public class FabricConfigureBuildPathCommandHandler extends JifConfigureBuildPathCommandHandler {

  public FabricConfigureBuildPathCommandHandler() {
    this(FabricPluginInfo.INSTANCE);
  }

  public FabricConfigureBuildPathCommandHandler(PluginInfo pluginInfo) {
    super(pluginInfo);
  }

}
