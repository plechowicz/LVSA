package vsa;

import java.util.ArrayList;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * Klasa slu��ca do wybrania odpowiedniego miksera z systemu do nagrywania i
 * odtwarzania d�wi�ku Je�eli zostanie wybrany mikser jako domy�lny, sta�a
 * okre�laj�ca mikser danego typu jest ustawiana na warto�� null. W przypadku
 * domy�lnego miksera TargetDataLine (odpowiednio SourceDataLine) jest
 * otrzymywana prosto z klasy AudioSystem.
 */
public class MixerChooser {

	/**
	 * -------------------------------------------------------------------------
	 * -------
	 * 
	 * Typ wyliczeniowy
	 * 
	 * -------------------------------------------------------------------------
	 * -------
	 */

	/**
	 * Typ wilyczeniowy okreslajacy czy zostal wybrany mikser czy pozostano przy
	 * domy�lnym.
	 */
	public enum MixerState {
		DEFAULT, CHOSE
	}

	/**
	 * -------------------------------------------------------------------------
	 * ---------
	 * 
	 * Pola klasy
	 * 
	 * -------------------------------------------------------------------------
	 * ---------
	 */

	/**
	 * Stan mikser�w - miksera wej�ciowego i miksera wyj�ciowego.
	 */
	private static MixerState inputMixerState = MixerState.DEFAULT;
	private static MixerState outputMixerState = MixerState.DEFAULT;

	/**
	 * Mikser wej�ciowy i wyj�ciowy.
	 */
	private static Mixer inputMixer = null;
	private static Mixer outputMixer = null;

	/**
	 * Listy dost�pnych mikser�w.
	 */
	private static ArrayList<Mixer.Info> inputMixersList = new ArrayList<Mixer.Info>();
	private static ArrayList<Mixer.Info> outputMixersList = new ArrayList<Mixer.Info>();

	static {
		getMixersListFromSystem(inputMixersList, outputMixersList);
	}

	/**
	 * ----------------------------------------------------------
	 * 
	 * Gettery i settery
	 * 
	 * ---------------------------------------------------------
	 */

	/**
	 * Zwraca tablic� String�w zawieraj�c� nazwy mikser�w wej�ciowych.
	 */
	public static String[] getInputMixersNames() {
		ArrayList<String> inputMixersNames = new ArrayList<String>();
		inputMixersNames.add("Default Input Device");
		for (int i = 0; i < inputMixersList.size(); i++)
			inputMixersNames.add(inputMixersList.get(i).getName());
		return inputMixersNames.toArray(new String[inputMixersNames.size()]);
	}

	/**
	 * Zwraca tablic� String�w zawieraj�c� nazwy mikser�w wyj�ciowych.
	 */
	public static String[] getOutputMixersNames() {
		ArrayList<String> outputMixersNames = new ArrayList<String>();
		outputMixersNames.add("Default Output Device");
		for (int i = 0; i < outputMixersList.size(); i++)
			outputMixersNames.add(outputMixersList.get(i).getName());
		return (String[]) outputMixersNames.toArray(new String[outputMixersNames.size()]);
	}

	/**
	 * Zwr�� stan miksera wej�ciowego.
	 * 
	 * @return stan miksera wej�ciowego
	 */
	public static MixerState getInputMixerState() {
		return inputMixerState;
	}

	/**
	 * Zwr�� stan miksera wyj�ciowego.
	 * 
	 * @return stan miksera wyj�ciowego
	 */
	public static MixerState getOutputMixerState() {
		return outputMixerState;
	}

	/**
	 * Ustawienie miksera o podanej nazwie jako urz�dzenie wej�ciowe.
	 * 
	 * @param name
	 *            nazwa miksera
	 */
	public static void setInputMixer(String name) {
		inputMixerState = MixerState.CHOSE;
		for (int i = 0; i < inputMixersList.size(); i++)
			if (inputMixersList.get(i).getName().equals(name))
				inputMixer = AudioSystem.getMixer(inputMixersList.get(i));

		if (inputMixer == null)
			inputMixerState = MixerState.DEFAULT;
	}

	/**
	 * Ustawienie miksera o podanej nazwie jako urz�dzenie wyj�ciowe.
	 * 
	 * @param name
	 *            nazwa miksera
	 */
	public static void setOutputMixer(String name) {
		outputMixerState = MixerState.CHOSE;
		for (int i = 0; i < outputMixersList.size(); i++)
			if (outputMixersList.get(i).getName().equals(name))
				outputMixer = AudioSystem.getMixer(outputMixersList.get(i));

		if (outputMixer == null)
			outputMixerState = MixerState.DEFAULT;
	}

	/**
	 * Ustawienie miksera wej�ciowego na domy�lny.
	 */
	public static void setDefaultInputMixer() {
		inputMixer = null;
		inputMixerState = MixerState.DEFAULT;
	}

	/**
	 * Ustawienie miksera wyj�ciowego na domy�lny.
	 */
	public static void setDefaultOutputMixer() {
		outputMixer = null;
		outputMixerState = MixerState.DEFAULT;
	}

	/**
	 * Zwr�� ustawiony mikser wej�ciowy.
	 */
	public static Mixer getInputMixer() {
		return inputMixer;
	}

	/**
	 * Zwr�� ustawiony mikser wyj�ciowy.
	 */
	public static Mixer getOutputMixer() {
		return outputMixer;
	}

	/**
	 * ------------------------------------------------------------------
	 * 
	 * Metody prywatne
	 * 
	 * -----------------------------------------------------------------
	 */

	/**
	 * Umieszczenie w listach, przechowuj�cych typ Mixer.Info, informacji o
	 * dostepnych mikserach w systemie, s�u��cych odpowiednio do przechwytywania
	 * d�wi�ku z wej�� i wysy�ynia na wyj�cia. Jezeli nie uda�o si� okre�li�
	 * zastosowania mikser�w, w obu listach s� zapisywane wszystkie typy.
	 */
	private static void getMixersListFromSystem(ArrayList<Mixer.Info> inputMixers, ArrayList<Mixer.Info> outputMixers) {
		inputMixers.clear();
		outputMixers.clear();

		Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		for (Mixer.Info mixerInfo : mixers) {
			String description = mixerInfo.getDescription();
			if (description.toLowerCase().contains("capture"))
				inputMixers.add(mixerInfo);
			else if (description.toLowerCase().contains("playback"))
				outputMixers.add(mixerInfo);
		}

		if (inputMixers.isEmpty() || outputMixers.isEmpty())
			for (Mixer.Info mixerInfo : mixers) {
				inputMixers.add(mixerInfo);
				outputMixers.add(mixerInfo);
			}
	}
}
