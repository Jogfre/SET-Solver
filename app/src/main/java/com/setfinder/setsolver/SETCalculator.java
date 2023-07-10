package com.setfinder.setsolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SETCalculator {
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

    public static String findComplement(String input1, String input2) {
        /*
        Takes two 4-digit codes and calculates the resulting code to create a SET.
        Example: 1123, 1132 will give the result:
         */
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
