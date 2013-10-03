/*******************************************************************************
* Copyright (c) 2008 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*******************************************************************************/

package fabric.imp.parser;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java_cup.runtime.lr_parser;

import org.eclipse.core.resources.IProject;
import org.eclipse.imp.parser.ILexer;
import org.eclipse.imp.parser.IParser;

import codebases.frontend.CodebaseSource;
import fabric.FabricScheduler;
import fabric.parse.Grm;
import fabric.parse.Lexer_c;
import fabric.parse.sym;
import lpg.runtime.ILexStream;
import lpg.runtime.IMessageHandler;
import lpg.runtime.IPrsStream;
import lpg.runtime.Monitor;
import polyglot.ast.Node;
import polyglot.frontend.FileSource;
//import polyglot.frontend.ForgivingVisitorGoal;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.CupParser;
import polyglot.frontend.Job;
import polyglot.frontend.Parser;
import polyglot.frontend.Scheduler;
import polyglot.frontend.Source;
import polyglot.lex.Lexer;
import polyglot.util.ErrorQueue;
//import x10.parser.X10Lexer;
//import x10.parser.X10SemanticRules;

/**
 * Information about our extension of the polyglot compiler. This derives from
 * the ExtensionInfo class used by the X10 compiler, and specializes it to create
 * a parser/scanner that can read from an arbitrary Reader, and to save the parser
 * and lexer for "interesting" source files for later reference. It also creates a
 * dummy goal to record the AST of "interesting" source files.
 * @author beth
 * @author rfuhrer@watson.ibm.com
 */
public class ExtensionInfo extends fabric.ExtensionInfo {
	public class FabricIDEParser extends CupParser implements IParser {
	    protected String[] termStrings;
		public FabricIDEParser(lr_parser grm, Source source, ErrorQueue eq) {
			super(grm, source, eq);
			// TODO Auto-generated constructor stub
		}

		public IPrsStream getIPrsStream() {
			return null;
		}

		@Override
		public int getEOFTokenKind() {
			return grm.EOF_sym();
		}

		private void setTermStrings() {
            Field[] fields = sym.class.getFields();
            termStrings = new String[fields.length];
            int i = 0;
            for (Field f : fields) {
                termStrings[i++] = f.getName();
            }
		}
		
		@Override
		public int numTokenKinds() {
		    if (termStrings == null) {
		        setTermStrings();
		    }
		    return termStrings.length;
		}

		@Override
		public String[] orderedTerminalSymbols() {	
		    if (termStrings == null) {
		        setTermStrings();
		    }
		    return termStrings;
		}

		@Override
		public Object parser(Monitor arg0, int arg1) {			
		}

		@Override
		public void reset(ILexStream arg0) {			
		}	
	}
	public class FabricIDELexer extends Lexer_c implements ILexer {

		private int[] kwKinds;

        public FabricIDELexer(InputStream in, FileSource file, ErrorQueue eq) {
			super(in, file, eq);
		}

		public FabricIDELexer(Reader reader, FileSource source, ErrorQueue eq) {
			super(reader, source, eq);
			// TODO Auto-generated constructor stub
		}

		@Override
		public ILexStream getILexStream() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int[] getKeywordKinds() {
		    if (kwKinds == null) {
    		    Map kws = this.keywords();
    		    kwKinds = new int[kws.entrySet().size()];
    		    
		    }
    			return null;
		}

		@Override
		public void initialize(char[] arg0, String arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void lexer(Monitor arg0, IPrsStream arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void reset(char[] arg0, String arg1) {
			// TODO Auto-generated method stub
			
		}
		
	}

    private final Monitor monitor;
    private final IMessageHandler handler;
    protected final Set<Source> fInterestingSources = new HashSet<Source>();
    private final Map<Source,Node> fInterestingASTs = new HashMap<Source,Node>();
    protected final Map<Source,Job> fInterestingJobs = new HashMap<Source,Job>();
    private final Map<Source,FabricIDEParser> fInterestingParsers = new HashMap<Source,FabricIDEParser>();
    private final Map<Source,FabricIDELexer> fInterestingLexers = new HashMap<Source,FabricIDELexer>();
    
    private final IProject project;
    
    public ExtensionInfo(Monitor monitor, IMessageHandler handler, IProject project) {
        this.monitor = monitor;
        this.handler = handler;
        this.project = project;
    }

    public void setInterestingSources(Collection<Source> sources) {
        fInterestingSources.clear();
        fInterestingJobs.clear();
        fInterestingASTs.clear();
        fInterestingLexers.clear();
        fInterestingParsers.clear();
        fInterestingSources.addAll(sources);
    }

    public FabricIDELexer getLexerFor(Source src) { return fInterestingLexers.get(src); }
    public FabricIDEParser getParserFor(Source src) { return fInterestingParsers.get(src); }
    public Node getASTFor(Source src) { Job job= fInterestingJobs.get(src); return (job != null) ? job.ast() : null; /* return fInterestingASTs.get(src); */ }
    public Job getJobFor(Source src) { return fInterestingJobs.get(src); }

    @Override
    protected Scheduler createScheduler() {
        return new FabricScheduler(this, filext);
        //XXX: what to the below do?
//        {
//            @Override
//            public List<Goal> goals(Job job) {
// 
//                if (fInterestingSources.contains(job.source())) {
//                	fInterestingJobs.put(job.source(), job);
//                }
//                List<Goal> goals =  super.semanticCheckSourceGoals(job);
//                List<Goal> newGoals = new ArrayList<Goal>();
//                if (project != null){
//                	for(Goal goal: goals){
//                		if (goal.name().equals("CheckASTForErrors")){ // --- WARNING: FRAGILE CODE HERE!
//                			newGoals.add(PackageDeclGoal(job, project));
//                		}
//                		newGoals.add(goal);
//                	}
//                	return newGoals;
//                }
//                return goals;
//            }
//            
//            
//            protected Goal PackageDeclGoal(Job job, IProject project){
//            	return new ForgivingVisitorGoal("PackageDeclarationCheck", job, new CheckPackageDeclVisitor(job, project)).intern(this);
//            }
// 
//        };
    }

    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
        CodebaseSource src = (CodebaseSource) source;
        FabricIDELexer lexer = new FabricIDELexer(reader, source, eq);
        Grm grm =
            new Grm(lexer, typeSystem(), nodeFactory(), eq,
                src.canonicalNamespace());
        FabricIDEParser parser = new FabricIDEParser(grm, source, eq);

        parser.getIPrsStream().setMessageHandler(handler);
        if (fInterestingSources.contains(source)) {
            fInterestingLexers.put(source, lexer);
            fInterestingParsers.put(source, parser);
        }
        return parser;
    }
}
