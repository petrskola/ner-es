/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ner;

/**
 *
 * @author Petr
 */
public class Main {
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws Exception{
		ESConnect escon = new ESConnect();
		NameTag nt = new NameTag(args[0]);
		escon.loadNerUpdate(nt);
		escon.endSession();
	}
}
