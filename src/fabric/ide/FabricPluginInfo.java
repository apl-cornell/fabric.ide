package fabric.ide;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;

import fabric.ExtensionInfo;
import jif.ide.JifPluginInfo;
import polyglot.ide.common.BuildpathUtil;

public class FabricPluginInfo extends JifPluginInfo {
  @SuppressWarnings("hiding")
  public static final FabricPluginInfo INSTANCE = new FabricPluginInfo();

  @Override
  public String pluginID() {
    return "fabric.ide";
  }

  @Override
  public String langName() {
    return "Fabric";
  }

  @Override
  public String langShortName() {
    return "Fabric";
  }

  @Override
  public ExtensionInfo makeExtInfo() {
    return new ExtensionInfo();
  }

  @Override
  public String natureID() {
    return "fabric.ide.fabricnature";
  }

  @Override
  public String builderId() {
    return "fabric.ide.fabricBuilder";
  }

  protected List<String> baseBootpath() {
    return new ArrayList<>(Arrays.asList(getPluginPath("/lib/fabric-rt/"),
        getPluginPath("/lib/fabric-rt.jar"), getPluginPath("/lib/fabric/"),
        getPluginPath("/lib/fabric.jar")));
  }

  @Override
  protected List<String> baseClasspath() {
    return Collections.emptyList();
  }

  @Override
  protected List<String> baseSigpath() {
    return new ArrayList<>(Arrays.asList(getPluginPath("/lib/fabric-sig/"),
        getPluginPath("/lib/fabric-sig.jar")));
  }

  protected List<String> baseFabILSigpath() {
    return new ArrayList<>(Arrays.asList(getPluginPath("/lib/fabric-il-sig/"),
        getPluginPath("/lib/fabric-il-sig.jar")));
  }

  @Override
  public List<String> addCompilerArgs(boolean validateOnly, IProject project,
      Collection<String> sourceFiles, List<String> result) {
    File buildpathFile = BuildpathUtil.buildpathFile(project);

    // Obtain the configured bootpath and append the base path.
    List<String> bootpath =
        BuildpathUtil.parse(this, buildpathFile, FabricPlugin.BOOTPATH);
    bootpath.addAll(baseBootpath());

    // Add the boot path option to the result.
    if (!bootpath.isEmpty()) {
      result.addAll(
          Arrays.asList("-bootclasspath", BuildpathUtil.flattenPath(bootpath)));
    }

    // Obtain the configured FabIL sigpath and obtain the base path.
    List<String> fabilSigpath =
        BuildpathUtil.parse(this, buildpathFile, FabricPlugin.FABIL_SIGPATH);
    fabilSigpath.addAll(baseFabILSigpath());

    // Add the FabIL sigpath option to the result.
    if (!fabilSigpath.isEmpty()) {
      result.addAll(
          Arrays.asList("-filsigcp", BuildpathUtil.flattenPath(fabilSigpath)));
    }
    
    // Compile with trusted providers. (XXX)
    result.add("-trusted-providers");

    return super.addCompilerArgs(validateOnly, project, sourceFiles, result);
  }

}
