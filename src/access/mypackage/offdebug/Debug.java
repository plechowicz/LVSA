package access.mypackage.offdebug;

/********************************************************
 * Klasa Debug, pakiet access.mypackage.offdebug
 * 
 * Klasa pomocnicza zawieraj¹ca pust¹ funkcjê.
 * Pakiet jest do³¹czany do koñcowego programu przeznaczonego
 * do u¿ytkowania. Pozwala to unikn¹æ szukania wszystkich miejsc,
 * w których program korzysta³ z klasy Debug
 * z pakietu access.mypackage.debug.
 * Wystarczy za³¹czyæ ten pakiet, ¿eby informacje u³atwiaj¹ce
 * pisanie programu nie by³y wyœwietlane na konsoli.
 * 
 * Porównaj z pakietem access.mypackage.debug
 * 
 ********************************************************
 */
public class Debug {

	/**
	 * Pusta metoda.
	 * @param message konieczne do³¹czenie parametru
	 * w celu przeci¹¿enia funkcji z innego pakietu. 
	 * W pakiecie
	 * acess.mypackage.debug
	 * w tej metodzie pojawia siê parametr String. 
	 */
	public static void debug(String message) {
		
	}

}
