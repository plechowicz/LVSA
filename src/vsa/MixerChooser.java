package vsa;

import java.util.ArrayList;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * Klasa slu¿¹ca do wybrania odpowiedniego miksera z systemu do nagrywania i odtwarzania dŸwiêku
 * Je¿eli zostanie wybrany mikser jako domyœlny, sta³a okreœlaj¹ca mikser danego typu jest ustawiana na wartoœæ null.
 * W przypadku domyœlnego miksera TargetDataLine (odpowiednio SourceDataLine) jest otrzymywana prosto z klasy AudioSystem.  
 */
public class MixerChooser {
	
	/**--------------------------------------------------------------------------------
	 * 
	 * Typ wyliczeniowy
	 * 
	 * --------------------------------------------------------------------------------
	 */
	
	/**
	 * Typ wilyczeniowy okreslajacy czy zostal wybrany mikser czy pozostano przy domyœlnym.
	 */
	public enum MixerState {
		DEFAULT, CHOSE
	}
	
	
	/**----------------------------------------------------------------------------------
	 * 
	 * Pola klasy
	 * 
	 * ----------------------------------------------------------------------------------
	 */
	
	/**
	 * Stan mikserów - miksera wejœciowego i miksera wyjœciowego.
	 */
	private static MixerState inputMixerState = MixerState.DEFAULT;
	private static MixerState outputMixerState = MixerState.DEFAULT;
	
	/**
	 * Mikser wejœciowy i wyjœciowy.
	 */
	private static Mixer inputMixer = null;
	private static Mixer outputMixer = null;
	
	/**
	 * Listy dostêpnych mikserów.
	 */
	private static ArrayList<Mixer.Info> inputMixersList = new ArrayList<Mixer.Info>();
	private static ArrayList<Mixer.Info> outputMixersList = new ArrayList<Mixer.Info>();
	
	static {
		getMixersListFromSystem(inputMixersList, outputMixersList);
	}
	
	
	/**----------------------------------------------------------
	 * 
	 *  Gettery i settery
	 *  
	 *  ---------------------------------------------------------
	 */
	

	/**
	 * Zwraca tablicê Stringów zawieraj¹c¹ nazwy mikserów wejœciowych.
	 */
	public static String[] getInputMixersNames() {
		ArrayList<String> inputMixersNames = new ArrayList<String>();
		inputMixersNames.add("Default Input Device");
		for(int i = 0; i < inputMixersList.size(); i++)
			inputMixersNames.add(inputMixersList.get(i).getName());
		return inputMixersNames.toArray(new String[inputMixersNames.size()]);
	}
	
	/**
	 * Zwraca tablicê Stringów zawieraj¹c¹ nazwy mikserów wyjœciowych.
	 */
	public static String[] getOutputMixersNames() {
		ArrayList<String> outputMixersNames = new ArrayList<String>();
		outputMixersNames.add("Default Output Device");
		for(int i = 0; i < outputMixersList.size(); i++)
			outputMixersNames.add(outputMixersList.get(i).getName());
		return (String[])outputMixersNames.toArray(new String[outputMixersNames.size()]);
	}
	
	/**
	 * Zwróæ stan miksera wejœciowego.
	 * @return stan miksera wejœciowego
	 */
	public static MixerState getInputMixerState() {
		return inputMixerState;
	}
	
	/**
	 * Zwróæ stan miksera wyjœciowego.
	 * @return stan miksera wyjœciowego
	 */
	public static MixerState getOutputMixerState() {
		return outputMixerState;
	}
	
	/**
	 * Ustawienie miksera o podanej nazwie jako urz¹dzenie wejœciowe.
	 * @param name nazwa miksera
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
	 * Ustawienie miksera o podanej nazwie jako urz¹dzenie wyjœciowe.
	 * @param name nazwa miksera
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
	 * Ustawienie miksera wejœciowego na domyœlny.
	 */
	public static void setDefaultInputMixer() {
		inputMixer = null;
		inputMixerState = MixerState.DEFAULT;
	}
	
	/**
	 * Ustawienie miksera wyjœciowego na domyœlny.
	 */
	public static void setDefaultOutputMixer() {
		outputMixer = null;
		outputMixerState = MixerState.DEFAULT;
	}
	
	/**
	 * Zwróæ ustawiony mikser wejœciowy.
	 */
	public static Mixer getInputMixer() {
		return inputMixer;
	}
	
	/**
	 * Zwróæ ustawiony mikser wyjœciowy.
	 */
	public static Mixer getOutputMixer() {
		return outputMixer;
	}
	
	
	/**------------------------------------------------------------------
	 * 
	 *  Metody prywatne
	 *  
	 *  -----------------------------------------------------------------
	 */
	
	/**
	 * Umieszczenie w listach, przechowuj¹cych typ Mixer.Info, informacji o dostepnych mikserach w systemie, 
	 * s³u¿¹cych odpowiednio do przechwytywania dŸwiêku z wejœæ i wysy³ynia na wyjœcia.
	 * Jezeli nie uda³o siê okreœliæ zastosowania mikserów, w obu listach s¹ zapisywane wszystkie typy. 
	 */
	private static void getMixersListFromSystem(ArrayList<Mixer.Info> inputMixers, ArrayList<Mixer.Info> outputMixers ) {
		inputMixers.clear();
		outputMixers.clear();
		
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		for (Mixer.Info mixerInfo : mixers){
			String description = mixerInfo.getDescription();
			if(description.toLowerCase().contains("capture"))
				inputMixers.add(mixerInfo);
			else if(description.toLowerCase().contains("playback"))
				outputMixers.add(mixerInfo);
		}
		
		if (inputMixers.isEmpty() || outputMixers.isEmpty())
			for (Mixer.Info mixerInfo : mixers){
				inputMixers.add(mixerInfo);
				outputMixers.add(mixerInfo);
			}
	}
}
