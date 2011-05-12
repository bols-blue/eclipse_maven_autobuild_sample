package example_get_tree.handlers;

import java.util.ArrayList;
import java.util.Iterator;

import jp.co.overtone.contenttype.elements.ElementFactory;
import jp.co.overtone.contenttype.elements.NSLContentElement;
import jp.co.overtone.contenttype.elements.NSLIncludeElement;
import jp.co.overtone.contenttype.elements.NSLModuleRootElement;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class GenarateHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public GenarateHandler() {
	}

	ArrayList<NSLContentElement> elementTree = new ArrayList<NSLContentElement>();
	IFile openFile;
	
	public void createImportElement() {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			if(openFile == null)return;
			manager.connect(openFile.getLocation(), LocationKind.LOCATION,
							null);
			ITextFileBuffer buffer = manager.getTextFileBuffer(openFile
					.getLocation(), LocationKind.LOCATION);
			IDocument document = buffer.getDocument();
			String tmp_name = "root";
			ElementFactory.createModuleRootElement(tmp_name, this.elementTree,
					document, openFile);
			Iterator<NSLContentElement> it = elementTree.iterator();
			while (it.hasNext()) {
				NSLContentElement module = it.next();
				if (module instanceof NSLModuleRootElement) {
					NSLModuleRootElement moduleRoot = (NSLModuleRootElement) module;
					moduleRoot.createModuleElement(tmp_name, this.elementTree, document);
				}else if (module instanceof NSLIncludeElement) {
					NSLIncludeElement includeRoot = (NSLIncludeElement) module;
					includeRoot.createImportElement();
				}
			}
		} catch (BadPositionCategoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPartitioningException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		ISelection selection = HandlerUtil.getCurrentSelection(event);
        
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			Object o = structured.getFirstElement();

	        //オブジェクトがIFileか調べる
	        if((o == null) || !(o instanceof IFile)){
	            return null;
	        }

	        //IFileなのでファイルとみなす
	        openFile = (IFile) o;
	        createImportElement();
		}
        
		Object[] obj = elementTree.get(0).getChildren();
		String tmp ="";
		for(int i=0;i < obj.length;i++){
			tmp += obj[i].toString()+"\n";
		}
		MessageDialog.openInformation(
				window.getShell(),
				"Example_get_tree",
				"get child\n"+tmp+selection);
		elementTree = new ArrayList<NSLContentElement>();
		return null;
	}
}
