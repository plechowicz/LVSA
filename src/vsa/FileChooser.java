package vsa;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;



/**
 * Opening, saving and converting file to correct format if possible. 
 */
@SuppressWarnings("serial")
public class FileChooser extends JFileChooser{
	
	/**
	 * State of the chosen file. 
	 * <p> At the beginning when no file is choose
	 * or errors occurs while opening a file the file state takes NO_FILE.
	 * If file is currently open and ready for actions the file state takes FILE_OPEN.
	 * If file is saving, file state takes FILE_SAVE to assured that no one is interrupting this. 
	 */
	enum FileState {
		NO_FILE, FILE_OPEN, FILE_SAVE;
	}
	
	
	/**
	 * Top frame of the application to blocking it when JFileChooser is open.
	 */
	private JFrame topFrame;	
	
	/**
	 * Path to file to open.
	 */
	private String filePathToOpen;
	/**
	 * Path to file to save.
	 */
	private String filePathToSave;
	
	/**
	 * State of file
	 */
	private volatile FileState fileState;
	
	/**
	 * Filtration of extensions
	 */
	private AudioFileFilter audioFilter;
	
	/**
	 * Initialize a file chooser.
	 * @param frame frame of the application
	 */
	FileChooser() {
		super(new File("."));
		topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
		filePathToOpen = null;
		filePathToSave = null;
		setFileState(FileState.NO_FILE);
		audioFilter = new AudioFileFilter();
		setFileFilter(audioFilter);
		setAcceptAllFileFilterUsed(false);
		
	}
	
	/**
	 * Open a File with JFileChooser
	 * If file is selected it is send to audioFileOpenerAndSaver to convert its format
	 * State of the file is stored in fileState.
	 */
	void choosePathToOpenFile() {
		
		if(showOpenDialog(topFrame) == JFileChooser.APPROVE_OPTION) {
			filePathToOpen = getSelectedFile().getAbsolutePath();
			setFileState((AudioFileOperations.openFile(filePathToOpen)? FileState.FILE_OPEN : FileState.NO_FILE ));
		}
	}
	
	/**
	 * Choose a directory and name to save a file. If wrong extension is given, 
	 * this method will correct it to the .wav extension.
	 */
	void choosePathToSaveFile() {
		if (showSaveDialog(topFrame) == JFileChooser.APPROVE_OPTION) {
			filePathToSave = getSelectedFile().getAbsolutePath();
			int i = filePathToSave.lastIndexOf('.');
			if(( i>0) && (i < filePathToSave.length() - 1)) {
				String ext = filePathToSave.substring(i+1).toLowerCase();
				if (!((ext.equals(Utils.wav)) || (ext.equals(Utils.wave)))) {
					ext = Utils.wav;
					filePathToSave = filePathToSave.substring(0, i + 1) + ext;
				}
			}
			else {
				filePathToSave = filePathToSave + "." + Utils.wav;
			}
			setFileState(FileState.FILE_SAVE);
		}
	
	}
	
	/**
	 * Pop up window asking whether to overwrite a file or not.
	 */
	@Override
	public void approveSelection() {
		File f = getSelectedFile();
		if(f.exists() && getDialogType() == SAVE_DIALOG) {
			int result = JOptionPane.showConfirmDialog(this, "The file exsists, overwrite?", "Exsisting file", JOptionPane.YES_NO_OPTION);
			switch(result) {
			case JOptionPane.YES_OPTION:
				super.approveSelection();
				return;
			case JOptionPane.NO_OPTION:
			case JOptionPane.CLOSED_OPTION:
				return;
			}
		}
		super.approveSelection();
	}
	
	
	/**
	 * Save audio byte data. Value of fileState and filePathToOpen depends on errors. 
	 * If File is successfully saved fileState gets value FILE_OPEN, and filePathToOpen gets value the same as filePathToSave.
	 * If errors occurred, fileState gets value FILE_OPEN if there was opened file before, but the filePathToOpen will not change its value.
	 * If previously no file were opened, fileState gets value NO_FILE.
	 * @param data
	 */
	void saveFile(byte[] data) {
		if(AudioFileOperations.saveFile(data, filePathToSave)) {
			setFileState(FileState.FILE_OPEN);
			filePathToOpen = filePathToSave;
		}
		else if (filePathToOpen != null)
			setFileState(FileState.FILE_OPEN);
		else
			setFileState(FileState.NO_FILE);
	}
	

	
	String getFilePathToOpen() {
		return filePathToOpen;
	}
	
	
	String getFilePathToSave() {
		return filePathToSave;
	}
	
	
	FileState getFileState() {
		return fileState;
	}
	
	void setFileState(FileState fileState) {
		synchronized(this) {
			this.fileState = fileState;
			notifyAll();
		}
	}
	
	/**
	 * Filter used for reducing possibility of opening wrong file. Filter hides
	 * files with different format that given in class Utils.
	 * The supported file formats are WAVE, AIFF, AU.
	 */
	class AudioFileFilter extends FileFilter {
		
		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = Utils.getExtension(f);
			if (extension != null) {
				if (extension.equals(Utils.wav)
				|| (extension.equals(Utils.wave))
				|| extension.equals(Utils.aif)
				|| extension.equals(Utils.aiff)
				|| extension.equals(Utils.aifc)
				|| extension.equals(Utils.au)
				|| extension.equals(Utils.snd)) 
					return true;
				else
					return false;
			}
			return false;
		}
		
		@Override
		public String getDescription() {
			return "WAVE, AIFF, AU";
		}
	}
	
	/**
	 * List of possible extensions of file to open.
	 */
	static class Utils {
		public final static String wav = "wav";
		public final static String wave = "wave";
		public final static String aif = "aif";
		public final static String aiff = "aiff";
		public final static String aifc = "aifc";
		public final static String au = "au";
		public final static String snd = "snd";
		
		/**
		 * Get extension of selected file in the JFileChooser.
		 * @param f file selected in JFileChooser
		 * @return String with the extension of file
		 */
		public static String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if( i>0 && i < s.length() - 1)
				ext = s.substring(i+1).toLowerCase();
			return ext;
		}
	}
}

