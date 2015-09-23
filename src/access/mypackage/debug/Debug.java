package access.mypackage.debug;

/********************************************************
 * Klasa Debug, pakiet access.mypackage.debug
 * 
 * Klasa pomocnicza s�u��ca do wypisywania informacji na konsol� podczas
 * testowania programu. W trakcie pracy nad projektem wystarczy zmieni� pakiet z
 * access.mypackage.offdebug na access.mypackage.debug w celu zobaczenia
 * informacji pomocniczych na konsoli.
 * 
 * Por�wnaj z pakietem access.mypackage.offdebug
 * 
 ********************************************************
 */

public class Debug {

	/**
	 * Wypisanie informacji na konsol�.
	 * 
	 * @param message
	 *            tre�� informacji
	 */
	public static void debug(String message) {
		System.out.println(message);
	}

}
