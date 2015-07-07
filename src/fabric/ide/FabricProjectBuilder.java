package fabric.ide;

import jif.ide.JifProjectBuilder;
import polyglot.ide.PluginInfo;

public class FabricProjectBuilder extends JifProjectBuilder {

  public FabricProjectBuilder() {
    super(FabricPluginInfo.INSTANCE);
  }

  public FabricProjectBuilder(PluginInfo pluginInfo) {
    super(pluginInfo);
  }

}
