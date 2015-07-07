package fabric.ide.editors;

import fabric.ide.FabricPluginInfo;
import jif.ide.editors.JifEditor;
import polyglot.ide.PluginInfo;

public class FabricEditor extends JifEditor {

  public FabricEditor() {
    this(FabricPluginInfo.INSTANCE);
  }

  public FabricEditor(PluginInfo pluginInfo) {
    super(pluginInfo);
  }

}
