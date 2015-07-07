package fabric.ide.natures;

import fabric.ide.FabricPluginInfo;
import jif.ide.natures.JifNature;
import polyglot.ide.PluginInfo;

public class FabricNature extends JifNature {

  public FabricNature() {
    super(FabricPluginInfo.INSTANCE);
  }

  public FabricNature(PluginInfo pluginInfo) {
    super(pluginInfo);
  }

}
