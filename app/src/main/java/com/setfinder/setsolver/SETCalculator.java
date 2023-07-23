package com.setfinder.setsolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SETCalculator {
    /**
     * <h3>calculateSET()</h3>
     * Will take an input HashMap containing the Card object, using the card properties code as a String key.<br>
     * Will then iterate through the HashMap and compute any containing SETs.<br>
     * @param validCards a HashMap containing the cards using their properties String as a key.
     * @return a HashSet containing any potential SETs.
     * @see com.setfinder.setsolver.Card
     */
    public static Set<Card[]> calculateSET(HashMap<String, Card> validCards) {
        String[] keys = validCards.keySet().toArray(new String[0]);
        final Set<Card[]> SET = new HashSet<>();

        if (keys.length > 2) {
            final HashMap<String, String> setMapOuter = new HashMap<>();
            for (String key : keys) {
                setMapOuter.put(key, key);
            }

            for (int i = 0; i < keys.length - 2; i++) {
                final String card1 = keys[i];
                final HashMap<String, String> setMapInner = new HashMap<>(setMapOuter);
                for (int j = i + 1; j < keys.length - 1; j++) {
                    final String card2 = keys[j];
                    final String complement = findComplement(card1, card2);
                    if (setMapInner.containsKey(complement)) {
                        Card[] set = new Card[]{
                                validCards.get(card1),
                                validCards.get(card2),
                                validCards.get(complement)
                        };
                        SET.add(set);
                        setMapInner.remove(complement);
                    }
                    setMapInner.remove(card2);
                }
                setMapOuter.remove(card1);
            }


        }
        return SET;
    }

    /**
     * <h3>findComplement()</h3>
     * This function will take two input strings of length 4 that are digits ranging from 1 to 3.<br>
     * It will then return the complementing string that will form a SET.<br>
     *
     * Example: inputs 1123 and 1132, will give the result: 1111
     * @param input1 the String containing the properties of the first card.
     * @param input2 the String containing the properties of the second card.
     * @return the String containing the properties of the complementary card.
     */
    public static String findComplement(String input1, String input2) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            int val1 = (int)input1.charAt(i) - 48;
            int val2 = (int)input2.charAt(i) - 48;

            if (val1 == val2) {
                output.append(val1);
                continue;
            }
            int negSum = (val1 + val2 + 1) * -1;
            int result = Math.floorMod(negSum, 3) + 1;

            output.append(result);
        }
        return output.toString();
    }
}
