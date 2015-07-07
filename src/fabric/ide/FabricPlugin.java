package fabric.ide;

import jif.ide.JifPlugin;
import polyglot.ide.common.BuildpathEntry;
import polyglot.ide.common.BuildpathEntry.Kind;

public class FabricPlugin extends JifPlugin {

  public static final Kind BOOTPATH =
      BuildpathEntry.Kind.get(FabricPlugin.class, "bootpath");
  
  public static final Kind FABIL_SIGPATH =
      BuildpathEntry.Kind.get(FabricPlugin.class, "fabil-sigpath");

}
