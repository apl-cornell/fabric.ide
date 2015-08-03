package fabric.ide.editors;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

import codebases.frontend.CodebaseSource;
import fabric.ExtensionInfo;
import fabric.lang.security.Label;
import fabric.lang.security.LabelUtil;
import polyglot.frontend.Source;
import polyglot.ide.editors.Editor;
import polyglot.ide.editors.ReconcilingStrategy;

public class FabricReconcilingStrategy extends ReconcilingStrategy {

  public FabricReconcilingStrategy(Editor editor) {
    super(editor);
  }

  @Override
  protected Source makeSource() {
    return new LocalDocumentSource();
  }

  protected class LocalDocumentSource extends DocumentSource
      implements CodebaseSource {

    protected URI namespace;

    public LocalDocumentSource() {
      this.namespace =
          ((ExtensionInfo) editor.makeExtInfo()).localNamespace();
    }

    @Override
    public URI namespace() {
      return namespace;
    }

    @Override
    public URI canonicalNamespace() {
      return namespace;
    }

    @Override
    public boolean shouldPublish() {
      return false;
    }

    @Override
    public void setPublish(boolean pub) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source derivedSource(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Source publishedSource(URI arg0, String arg1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Label label() {
      return LabelUtil._Impl.noComponents();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof LocalDocumentSource) {
        LocalDocumentSource s = (LocalDocumentSource) obj;
        return toUri().toString().equalsIgnoreCase(s.toUri().toString());
      }

      return false;
    }

    @Override
    public int hashCode() {
      return toUri().toString().toLowerCase().hashCode();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Reader open() throws IOException {
      return openReader(false);
    }

  }
}
