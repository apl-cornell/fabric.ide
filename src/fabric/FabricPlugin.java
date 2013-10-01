package fabric;

import org.eclipse.imp.runtime.PluginBase;
import org.osgi.framework.BundleContext;

public class FabricPlugin extends PluginBase {

	public static final String kPluginID = "FabricIDE";
	public static final String kLanguageID = "Fabric";

	public static final String FAB_PRJ_JAVA_NATURE_ID = "edu.cornell.cs.apl.fabricNature";
	/**
	 * The unique instance of this plugin class
	 */
	protected static FabricPlugin sPlugin;

	public static FabricPlugin getInstance() {
		if (sPlugin == null)
			new FabricPlugin();
		return sPlugin;
	}

	public FabricPlugin() {
		super();
		sPlugin = this;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	@Override
	public String getID() {
		return kPluginID;
	}

	@Override
	public String getLanguageID() {
		return kLanguageID;
	}
}
