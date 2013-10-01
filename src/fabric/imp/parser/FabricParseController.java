package fabric.imp.parser;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import lpg.runtime.IPrsStream;
import lpg.runtime.IToken;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.imp.core.ErrorHandler;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IMessageHandler;
import org.eclipse.imp.parser.ISourcePositionLocator;
import org.eclipse.imp.parser.SimpleLPGParseController;
import org.eclipse.imp.services.ILanguageSyntaxProperties;
import org.eclipse.jface.text.IRegion;

import fabric.FabricPlugin;
import fabric.imp.parser.ExtensionInfo.FabricIDELexer;
import fabric.imp.parser.ExtensionInfo.FabricIDEParser;
import polyglot.ast.Node;
import polyglot.frontend.FileSource;
import polyglot.frontend.Job;
import polyglot.frontend.Source;
//import polyglot.frontend.ZipResource;
import polyglot.util.ErrorInfo;

public class FabricParseController extends SimpleLPGParseController {
//	/**
//	 * A trivial extension of the class ZipResource that permits the user to provide the
//	 * source text as an explicit parameter, rather than reading it from the .zip file
//	 * itself. Useful for representing the contents of editor buffers that reside in
//	 * zip files, even when the editor buffers are read-only.<br>
//	 * Along with StringSource and StringResource, probably belongs in Polyglot, not here.
//	 * @author rfuhrer
//	 */
//	public static final class ZipStringResource extends ZipResource {
//		private final String contents;
//
//		private ZipStringResource(File source, ZipFile zip, String entryName, String contents) {
//			super(source, zip, entryName);
//			this.contents = contents;
//		}
//
//		@Override
//		public InputStream getInputStream() throws IOException {
//			return new StringBufferInputStream(contents);
//		}
//	}
//
	public interface InvariantViolationHandler {
		public void clear();
		public void handleViolation(ErrorInfo error);
		public void consumeAST(Node root);
	}

	private static final Pattern JAR_IDENTIFIER_PATTERN = Pattern.compile(".*\\.jar:.*");

	private CompilerDelegate fCompiler;
    private PMMonitor fMonitor;
    private InvariantViolationHandler fViolationHandler;

	private PolyglotNodeLocator fSourcePositionLocator;

    public FabricParseController() {
    	super(FabricPlugin.kLanguageID);
    }

    public CompilerDelegate getCompiler() {
        return fCompiler;
    }

    @Override
    public ISourcePositionLocator getSourcePositionLocator() {
        if (fSourcePositionLocator == null) {
            fSourcePositionLocator= new PolyglotNodeLocator(fProject, getLexStream());
        }
        return fSourcePositionLocator;
    }

    public ILanguageSyntaxProperties getSyntaxProperties() {
        return new X10SyntaxProperties();
    }

    @Override
    public void initialize(IPath filePath, ISourceProject project, IMessageHandler handler) {
        super.initialize(filePath, project, handler);
        fMonitor= new PMMonitor(null);
    }

    public void setViolationHandler(InvariantViolationHandler handler) {
    	fViolationHandler= handler;
    }
    
    public Object parse(final String contents, IProgressMonitor monitor) {
    	Source source= null;

    	try {
            fMonitor.setMonitor(monitor);

            int jarPathComponentIdx = findJarIdentifierComponent(fFilePath);

            if (jarPathComponentIdx >= 0) {
            	source = buildJarFileEntrySource(contents, jarPathComponentIdx);
            } else {
            	String path= fProject != null ? fProject.getRawProject().getLocation().append(fFilePath).toOSString() : fFilePath.toOSString();
            	File file= new File(path);

            	source= new FileSource(new StringResource(contents, file, path));
            }

            List<Source> streams= Arrays.asList(source);
            IProject proj= (fProject != null) ? fProject.getRawProject() : null;
            IPath sourcePath = (fProject != null) ? Platform.getLocation().append(fProject.getName()).append(fFilePath) : fFilePath;

            fCompiler= new CompilerDelegate(fMonitor, handler, proj, sourcePath, fViolationHandler); // Create the compiler
            fCompiler.compile(streams);
    	} catch (FileNotFoundException e) {
    	    // do nothing - presumably the file just got deleted...
        } catch (IOException e) {
//            X10DTUIPlugin.log(e);
        } catch (CoreException e) {
//            X10DTUIPlugin.log(e);
        } finally {
        
            // RMF 8/2/2006 - retrieve the AST, token stream and lex stream, if they exist; front-end semantic
            // checks may fail, even though the AST/token-stream are well-formed enough to support various IDE
        	// services, like syntax highlighting and the outline view's contents.

            if (source != null && fCompiler != null) {
            	final FabricIDEParser parser= fCompiler.getParserFor(source);
            	final FabricIDELexer lexer= fCompiler.getLexerFor(source);
            	fParseStream = parser.getIPrsStream();
            	fLexStream = fParseStream.getILexStream();
//            	fLexStream = lexer.getILexStream();
            	fParser = new ParserDelegate(parser); // HACK - SimpleLPGParseController.cacheKeywordsOnce() needs an IParser and an ILexer, so create them here. Luckily, they're just lightweight wrappers...
            	fLexer = new LexerDelegate(lexer);
            	fCurrentAst= fCompiler.getASTFor(source); // getASTFor(fileSource); // TODO use commandLineJobs() instead?

            	if (fViolationHandler != null && fCurrentAst != null) {
                    // TODO Tweak appropriate option in Configuration/Options object to include the invariant checking goals
	            	Job job= fCompiler.getJobFor(source);
	            	PositionInvariantChecker pic= new PositionInvariantChecker(job, "", true);
	            	InstanceInvariantChecker iic= new InstanceInvariantChecker(job);

	            	((Node) fCurrentAst).visit(pic);
	            	((Node) fCurrentAst).visit(iic);

	            	fViolationHandler.consumeAST((Node) fCurrentAst);
	            }
        	}
            // RMF 8/2/2006 - cacheKeywordsOnce() must have been run for syntax highlighting to work.
            // Must do this after attempting parsing (even though that might fail), since it depends
            // on the parser/lexer being set in the ExtensionInfo, which only happens as a result of
            // ExtensionInfo.parser(). Ugghh.
            if (fParser != null) {
            	cacheKeywordsOnce();
            }
            fCompiler = null;
            fParser = null;
            fLexer = null;
        }
        return fCurrentAst;
    }

    /**
     * Build a Polyglot Source object that refers to the jar file entry given by fFilePath,
     * but using the given String as the source text contents.
     * @param jarPathComponentIdx the index of the path component that contains the "jar:" indicator
     */
	private Source buildJarFileEntrySource(final String contents, int jarPathComponentIdx) throws ZipException, IOException {
		Source source;
		String jarPathComponent= fFilePath.segment(jarPathComponentIdx);
		StringBuilder jarPath= new StringBuilder();

		for(int i=0; i < jarPathComponentIdx; i++) {
			jarPath.append(File.separatorChar);
			jarPath.append(fFilePath.segment(i));
		}

		String jarName= jarPathComponent.substring(0, jarPathComponent.indexOf(':'));
		String trailer= jarPathComponent.substring(jarPathComponent.indexOf(':') + 1);

		jarPath.append(File.separatorChar);
		jarPath.append(jarName);

		StringBuilder entryPath= new StringBuilder();

		entryPath.append(trailer);
		for(int i=jarPathComponentIdx+1; i < fFilePath.segmentCount(); i++) {
			entryPath.append('/');
			entryPath.append(fFilePath.segment(i));
		}

		File jarFile= new File(jarPath.toString());
		ZipFile zipFile= new ZipFile(jarFile);
		ZipResource zipRsrc= new ZipStringResource(jarFile, zipFile, entryPath.toString(), contents);

		source= new FileSource(zipRsrc);
		return source;
	}

    /**
     * @return the index of the given IPath that indicates that the path refers
     * to a jar file entry, if any, or -1 if this is not a jar file entry path
     */
	private int findJarIdentifierComponent(IPath filePath) {
		int jarPathComponentIdx= -1;
		for(int i=0; i < filePath.segmentCount(); i++) {
			String seg= filePath.segment(i);
			if (JAR_IDENTIFIER_PATTERN.matcher(seg).matches()) {
				jarPathComponentIdx= i;
				break;
			}
		}
		return jarPathComponentIdx;
	}

    // TODO Use this rather than fCompiler.getASTFor() ? (more reliable?)
    private Object getASTFor(Source source) {
    	Collection<Job> cmdJobs = fCompiler.getExtInfo().scheduler().commandLineJobs();
    	for(Job job: cmdJobs) {
    		if (job.source().equals(source)) {
    			return job.ast();
    		}
    	}
    	return null;
    }

    @Override
    public Iterator<IToken> getTokenIterator(IRegion region) {
      final int regionOffset= region.getOffset();
      final int regionLength= region.getLength();
      final int regionEnd= regionOffset + regionLength - 1;

      if (fParseStream == null) {
    	  return new Iterator<IToken>() {
			public boolean hasNext() {
				return false;
			}

			public IToken next() {
				return null;
			}

			public void remove() {
			} };
      }
      return new Iterator<IToken>() {
          final IPrsStream stream= fParseStream;
          final int firstTokIdx= getTokenIndexAtCharacter(regionOffset);
          final int lastTokIdx;
          {
              int endIdx= getTokenIndexAtCharacter(regionEnd);
              char[] streamChars= stream.getInputChars();
              int streamLen= streamChars.length;
              try {
                  if (regionEnd >= 1 && regionEnd < streamLen
                          && streamChars[regionEnd] == IToken.EOF) {
                      // skip EOF token (assume LPG puts one at end of input
                      // character stream, since it does)
                      endIdx--;
                  }
              } catch (ArrayIndexOutOfBoundsException e) {
                  ErrorHandler.logError("ParseController.getTokenIterator(IRegion): error initializing lastTokIdx",
                          e);
                  // System.err.println("getTokenIterator: new Iterator(..)<init>: ArrayIndexOutOfBoundsException");
                  // System.err.println(" regionEnd = " + regionEnd + ", endIdx = " + endIdx + ", streamLen = " + streamLen + ",
                  // inputChars.length = " + streamChars.length);
              }
              lastTokIdx= endIdx;
          }
          int curTokIdx= Math.max(1, firstTokIdx); // skip bogus initial token

          private int getTokenIndexAtCharacter(int offset) {
              int result= stream.getTokenIndexAtCharacter(offset);
              // getTokenIndexAtCharacter() answers the negative of the index of the
              // preceding token if the given offset is not actually within a token.
              if (result < 0) {
                  result= -result + 1;
              }

              // The above may leave result set to a value that is one more than the
              // last token index, so return the last token index if that's the case
              // (This can happen if the end of the file contains some text that
              // does not correspond to a token--e.g., if the text represents an adjunct
              // or something unrecognized)
              if (result >= stream.getTokens().size())
                  result= stream.getTokens().size() - 1;

              return result;
          }

          // The following declarations cover the whole input stream, which
          // may be a proper superset of the range of the given region.
          // For now, that's a simple way to collect the information, and
          // most often the given region corresponds to the whole input anyway.
          // In any case, iteration is based on the range of the given region.

          // The preceding adjuncts for each token
          IToken[][] precedingAdjuncts= new IToken[lastTokIdx + 1][];
          {
              stream.setStreamLength();
              for(int i= 0; i < precedingAdjuncts.length; i++) {
                  precedingAdjuncts[i]= stream.getPrecedingAdjuncts(i);
              }
          }

          // The current indices for each array of preceding adjuncts
          int[] nextPrecedingAdjunct= new int[lastTokIdx + 1];
          {
              for(int i= 0; i < nextPrecedingAdjunct.length; i++) {
                  if (precedingAdjuncts[i].length == 0)
                      nextPrecedingAdjunct[i]= -1;
                  else
                      nextPrecedingAdjunct[i]= 0;
              }
          }

          // The following adjuncts (for the last token only)
          IToken[] followingAdjuncts;
          {
              if (lastTokIdx <= 0)
                  followingAdjuncts= new IToken[0];
              else
                  followingAdjuncts= stream.getFollowingAdjuncts(lastTokIdx);
          }

          // The current index for the array of following adjuncts
          int nextFollowingAdjunct;
          {
              if (followingAdjuncts.length == 0)
                  nextFollowingAdjunct= -1;
              else
                  nextFollowingAdjunct= 0;
          }

          // To support hasNext(); initial values may be reset if appropriate
          private boolean finalTokenReturned= regionEnd < 1 || lastTokIdx <= 0;
          private boolean finalAdjunctsReturned= !(followingAdjuncts.length > 0);

          /**
           * Tests whether the iterator has any unreturned tokens. These may
           * include "regular" tokens and "adjunct" tokens (e.g., representing
           * comments).
           * 
           * @return True if there is another token available, false otherwise
           */
          public boolean hasNext() {
              return !(finalTokenReturned && finalAdjunctsReturned);
          }

          /**
           * Returns the next available token in the iterator (or null if
           * there is none)
           * 
           * Will return a valid token under conditions that would cause
           * hasNext() to to return true; conversely, will return null under
           * conditions that would cause hasNext() to return false.
           * 
           * As a side effect, updates the flags that are used to compute the
           * value returned by hasNext().
           * 
           * The returned token may be a "regular" token (which will have a
           * corresponding AST node) or an "adjunct" token (which will
           * represent a comment). The tokens are returned in the order in
           * which they occur in the text, regardless of their kind.
           * 
           */
          public IToken next() {
              int next= -1; // for convenience

              // If we're not all the way through the tokens
              if (curTokIdx <= lastTokIdx) {

                  // First check for any remaining preceding adjuncts
                  // of the current token
                  next= nextPrecedingAdjunct[curTokIdx];
                  // If the current token has any unreturned preceding
                  // adjuncts
                  if (next >= 0 && next < precedingAdjuncts[curTokIdx].length) {
                      // Return the next preceding adjunct, incrementing the
                      // adjunct index afterwards
                      return precedingAdjuncts[curTokIdx][nextPrecedingAdjunct[curTokIdx]++];
                  }

                  // Flag whether the current token is the last one
                  finalTokenReturned= curTokIdx >= lastTokIdx;

                  // Return the current token, incrementing the token index
                  // afterwards
                  return stream.getIToken(curTokIdx++);
              }

              // If there are any adjuncts following the last token
              if (nextFollowingAdjunct >= 0 && nextFollowingAdjunct < followingAdjuncts.length) {
                  // Flag whether the current adjunct is the last one
                  finalAdjunctsReturned= (nextFollowingAdjunct + 1) >= followingAdjuncts.length;

                  // Return the current adjunct, incrementing the adjunct
                  // index afterwards
                  return followingAdjuncts[nextFollowingAdjunct++];
              }

              return null;
          }

          public void remove() {
              throw new UnsupportedOperationException("Unimplemented");
          }
      };
    }
}

//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.List;
//
//import lpg.runtime.IPrsStream;
//import lpg.runtime.IToken;
//import lpg.runtime.LexStream;
//import lpg.runtime.LpgLexStream;
//import lpg.runtime.Monitor;
//import lpg.runtime.PrsStream;
//
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.imp.model.ISourceProject;
//import org.eclipse.imp.parser.ILexer;
//import org.eclipse.imp.parser.IMessageHandler;
//import org.eclipse.imp.parser.IParseController;
//import org.eclipse.imp.parser.IParser;
//import org.eclipse.imp.parser.ISourcePositionLocator;
//import org.eclipse.imp.parser.MessageHandlerAdapter;
//import org.eclipse.imp.parser.SimpleLPGParseController;
//import org.eclipse.imp.services.ILanguageSyntaxProperties;
//import fabric.FabricPlugin;
//
///**
// * NOTE:  This version of the Parse Controller is for use when the Parse
// * Controller and corresponding Node Locator are generated separately from
// * a corresponding set of LPG grammar templates and possibly in the absence
// * of the lexer, parser, and AST-related types that would be generated from
// * those templates.  It is assumed that either a) the Controller will be
// * used with a suitable set of lexer, parser, and AST-related types
// * that are provided by some means other than LPG, or b) the Controller will
// * be used with a set of lexer, parser, and AST types that have been, or will
// * be, separately generated based on LPG.  In order to enable this version of
// * the Parse Controller to compile, dummy lexer, parser, and AST-related types
// * have been included as member types in the Controller.  These types are not
// * operational and are merely placeholders for types that would support a
// * functioning implementation.  Apart from the inclusion of these dummy types,
// * this representation of the Parse Controller is the same as that used
// * with LPG.
// * 	
// * @author Stan Sutton (suttons@us.ibm.com)
// * @since May 1,  2007	Addition of marker types
// * @since May 10, 2007	Conversion IProject -> ISourceProject
// * @since May 15, 2007	Addition of dummy types
// */
//public class FabricParseController extends SimpleLPGParseController implements
//		IParseController {
//
//	public FabricParseController() {
//		super(FabricPlugin.kLanguageID);
//	}
//
//	public class ASTNodeToken extends ASTNode implements IASTNodeToken {
//		public ASTNodeToken(IToken token) {
//			super(token);
//		}
//	}
//
//	public interface Visitor {
//		boolean preVisit(ASTNode element);
//
//		void postVisit(ASTNode element);
//
//		boolean visit(ASTNodeToken n);
//
//		void endVisit(ASTNodeToken n);
//	}
//
//	public static abstract class AbstractVisitor implements Visitor {
//		public abstract void unimplementedVisitor(String s);
//
//		public boolean preVisit(ASTNode element) {
//			return true;
//		}
//
//		public void postVisit(ASTNode element) {
//		}
//
//		public boolean visit(ASTNodeToken n) {
//			unimplementedVisitor("visit(ASTNodeToken)");
//			return true;
//		}
//
//		public void endVisit(ASTNodeToken n) {
//			unimplementedVisitor("endVisit(ASTNodeToken)");
//		}
//	}
//
//	public interface IASTNodeToken {
//		public IToken getLeftIToken();
//
//		public IToken getRightIToken();
//
//		void accept(Visitor v);
//	}
//
//	public class ASTNode implements IASTNodeToken {
//		public ASTNode(IToken token) {
//		}
//
//		public ASTNode(IToken leftIToken, IToken rightIToken) {
//		}
//
//		public IToken getLeftIToken() {
//			return null;
//		}
//
//		public IToken getRightIToken() {
//			return null;
//		}
//
//		public void accept(Visitor v) {
//		}
//	}
//
//	public class FabricLexer extends LpgLexStream implements ILexer {
//		public FabricLexer(String filename) throws java.io.IOException {
//		}
//
//		public LexStream getLexStream() {
//			return (LexStream) this;
//		}
//
//		public void lexer(Monitor monitor, IPrsStream prsStream) {
//		};
//
//		public int[] getKeywordKinds() {
//			return null;
//		}
//
//		public int getKind(int i) {
//			return 0;
//		}
//
//		public String[] orderedExportedSymbols() {
//			return null;
//		}
//	}
//
//	public class FabricParser extends PrsStream implements IParser {
//		public int numTokenKinds() {
//			return 0;
//		}
//
//		public FabricParser(LexStream lexStream) {
//		}
//
//		public PrsStream getParseStream() {
//			return (PrsStream) this;
//		}
//
//		public Object parser(Monitor monitor, int error_repair_count) {
//			return null;
//		}
//
//		public int getEOFTokenKind() {
//			return 0;
//		}
//
//		public void setMessageHandler(IMessageHandler errMsg) {
//		}
//
//		public void resolve(ASTNode root) {
//		}
//	}
//
//	private FabricParser parser;
//	private FabricLexer lexer;
//	private ASTNode currentAst;
//
//	private char keywords[][];
//	private boolean isKeyword[];
//
//	/**
//	 * @param filePath		Project-relative path of file
//	 * @param project		Project that contains the file
//	 * @param handler		A message handler to receive error messages (or any others)
//	 * 						from the parser
//	 */
//	public void initialize(IPath filePath, ISourceProject project,
//			IMessageHandler handler) {
//		super.initialize(filePath, project, handler);
//		IPath fullFilePath = project.getRawProject().getLocation()
//				.append(filePath);
//		createLexerAndParser(fullFilePath);
//
//		parser.setMessageHandler(handler);
//	}
//
//	public IParser getParser() {
//		return parser;
//	}
//
//	public ILexer getLexer() {
//		return lexer;
//	}
//
//	public ISourcePositionLocator getNodeLocator() {
//		return new FabricASTNodeLocator();
//	}
//
//	public ILanguageSyntaxProperties getSyntaxProperties() {
//		return null;
//	}
//
//	private void createLexerAndParser(IPath filePath) {
//		try {
//			lexer = new FabricLexer(filePath.toOSString());
//			parser = new FabricParser(lexer.getLexStream());
//		} catch (IOException e) {
//			throw new Error(e);
//		}
//	}
//
//	/**
//	 * setFilePath() should be called before calling this method.
//	 */
//	public Object parse(String contents, boolean scanOnly,
//			IProgressMonitor monitor) {
//		PMMonitor my_monitor = new PMMonitor(monitor);
//		char[] contentsArray = contents.toCharArray();
//
//		lexer.initialize(contentsArray, fFilePath.toPortableString());
//		parser.getParseStream().resetTokenStream();
//
//		lexer.lexer(my_monitor, parser.getParseStream()); // Lex the stream to produce the token stream
//		if (my_monitor.isCancelled())
//			return currentAst; // TODO currentAst might (probably will) be inconsistent wrt the lex stream now
//
//		currentAst = (ASTNode) parser.parser(my_monitor, 0);
//		parser.resolve(currentAst);
//
//		cacheKeywordsOnce();
//
//		return currentAst;
//	}
//
//}
