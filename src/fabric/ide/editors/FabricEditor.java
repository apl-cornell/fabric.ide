package fabric.ide.editors;

import fabric.ide.FabricPluginInfo;
import jif.ide.editors.JifEditor;
import polyglot.ide.PluginInfo;
import polyglot.ide.editors.SourceViewerConfiguration;

public class FabricEditor extends JifEditor {

  public FabricEditor() {
    this(FabricPluginInfo.INSTANCE);
  }

  public FabricEditor(PluginInfo pluginInfo) {
    super(pluginInfo);
  }

  @Override
  protected SourceViewerConfiguration createSourceViewerConfiguration() {
    return new FabricSourceViewerConfiguration(this, colorManager);
  }

}
