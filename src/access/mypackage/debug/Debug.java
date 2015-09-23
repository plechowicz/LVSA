package access.mypackage.debug;

/********************************************************
 * Klasa Debug, pakiet access.mypackage.debug
 * 
 * Klasa pomocnicza s³u¿¹ca do wypisywania informacji
 * na konsolê podczas testowania programu.
 * W trakcie pracy nad projektem wystarczy zmieniæ pakiet
 * z access.mypackage.offdebug na access.mypackage.debug
 * w celu zobaczenia informacji pomocniczych na konsoli.
 * 
 * Porównaj z pakietem access.mypackage.offdebug
 * 
 ********************************************************
 */

public class Debug {
	
	/**
	 * Wypisanie informacji na konsolê.
	 * @param message treœæ informacji
	 */
	public static void debug(String message) {
		System.out.println(message);
	}

}
