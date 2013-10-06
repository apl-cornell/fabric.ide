package fabric.imp.parser;

import lpg.runtime.IMessageHandler;
import lpg.runtime.Monitor;

import org.eclipse.core.resources.IProject;

import polyglot.frontend.Scheduler;
import fabric.FabricScheduler;

public class ParseExtensionInfo extends ExtensionInfo {
	
	public ParseExtensionInfo(Monitor monitor, IMessageHandler handler, IProject project){
		super(monitor, handler, project);
	}

	@Override
    protected Scheduler createScheduler() {
        return new FabricScheduler(this, filext);
        //XXX: what should the below do?
//        return new FabricScheduler(this) {
//            @Override
//            public List<Goal> goals(Job job) {
//                if (fInterestingSources.contains(job.source())) {
//                	fInterestingJobs.put(job.source(), job);
//                }
//                return super.parseSourceGoals(job);
//            }
// 
//        };
    }
}
