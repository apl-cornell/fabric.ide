package fabric.ide.editors;

import org.eclipse.jface.text.reconciler.IReconcilingStrategy;

import polyglot.ide.editors.ColorManager;
import polyglot.ide.editors.Editor;
import polyglot.ide.editors.SourceViewerConfiguration;

public class FabricSourceViewerConfiguration extends SourceViewerConfiguration {

  public FabricSourceViewerConfiguration(Editor editor,
      ColorManager colorManager) {
    super(editor, colorManager);
  }

  @Override
  protected IReconcilingStrategy getReconcilingStrategy() {
    return new FabricReconcilingStrategy(editor);
  }

}
