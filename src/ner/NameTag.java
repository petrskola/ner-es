
package ner;
// This file is part of NameTag.
//
// Copyright 2013 by Institute of Formal and Applied Linguistics, Faculty of
// Mathematics and Physics, Charles University in Prague, Czech Republic.
//
// NameTag is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as
// published by the Free Software Foundation, either version 3 of
// the License, or (at your option) any later version.
//
// NameTag is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with NameTag.  If not, see <http://www.gnu.org/licenses/>.

import cz.cuni.mff.ufal.nametag.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Stack;

class NameTag {
	private Tokenizer tokenizer;
	private Ner ner;
	
	public NameTag(String nerFile){
		prepareNer(nerFile);
	}
	
  public static String encodeEntities(String text) {
    return text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
  }

  public static void sortEntities(NamedEntities entities, ArrayList<NamedEntity> sortedEntities) {
    class NamedEntitiesComparator implements Comparator<NamedEntity> {
      public int compare(NamedEntity a, NamedEntity b) {
        if (a.getStart() < b.getStart()) return -1;
        if (a.getStart() > b.getStart()) return 1;
        if (a.getLength() > b.getLength()) return -1;
        if (a.getLength() < b.getLength()) return 1;
        return 0;
      }
    }
    NamedEntitiesComparator comparator = new NamedEntitiesComparator();

    sortedEntities.clear();
    for (int i = 0; i < entities.size(); i++)
      sortedEntities.add(entities.get(i));
    Collections.sort(sortedEntities, comparator);
  }

  public void prepareNer(String nerFile) {
    if (nerFile.length() == 0) {
      System.err.println("Usage: RunNer recognizer_model");
      System.exit(1);
    }

    System.err.print("Loading ner: ");
    this.ner = Ner.load(nerFile);
    if (ner == null) {
      System.err.println("Cannot load recognizer from file '" + nerFile + "'");
      System.exit(1);
    }
    System.err.println("done");

    this.tokenizer = ner.newTokenizer();
    if (tokenizer == null) {
      System.err.println("No tokenizer is defined for the supplied model!");
      System.exit(1);
    }
	}
	
	public String doNer(String text) {
		Forms forms = new Forms();
		TokenRanges tokens = new TokenRanges();
		NamedEntities entities = new NamedEntities();
		ArrayList<NamedEntity> sortedEntities = new ArrayList<NamedEntity>();
		Scanner reader = new Scanner(System.in);
		Stack<Integer> openEntities = new Stack<Integer>();
		String out = "";
		// Tokenize and recognize
		tokenizer.setText(text);
		int unprinted = 0;
		while (tokenizer.nextSentence(forms, tokens)) {
			ner.recognize(forms, entities);
			sortEntities(entities, sortedEntities);

			for (int i = 0, e = 0; i < tokens.size(); i++) {
				TokenRange token = tokens.get(i);
				int token_start = (int)token.getStart();
				int token_end = (int)token.getStart() + (int)token.getLength();

				if (unprinted < token_start){ 
					//System.out.print(encodeEntities(text.substring(unprinted, token_start)));
					out = out + encodeEntities(text.substring(unprinted, token_start));
				}
				//if (i == 0) out = out + " " + "<sentence>";

				// Open entities starting at current token
				for (; e < sortedEntities.size() && sortedEntities.get(e).getStart() == i; e++) {
					//System.out.printf("<ne type=\"%s\">", sortedEntities.get(e).getType());
					//out = out + " " + "<ne type=\"" + sortedEntities.get(e).getType() + "\">";
					openEntities.push((int)sortedEntities.get(e).getStart() + (int)sortedEntities.get(e).getLength() - 1);
				}

				// The token itself
				//System.out.printf("<token>%s</token>", encodeEntities(text.substring(token_start, token_end)));
				out = out + " " + /*"<token>" + */ encodeEntities(text.substring(token_start, token_end)) /* + "</token>"*/;

				// Close entities ending after current token
				while (!openEntities.empty() && openEntities.peek() == i) {
					//out = out + " " + "</ne>";
					openEntities.pop();
				}
				//if (i + 1 == tokens.size()) out = out + " " + "</sentence>";
				unprinted = token_end;
			}
		}
    return out;
  }
}