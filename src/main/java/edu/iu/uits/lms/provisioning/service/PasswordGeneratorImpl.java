package edu.iu.uits.lms.provisioning.service;

import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Class to generate passwords
 *
 * @see <a href="http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string">thie article</a> for some more details.
 */
@Service
public class PasswordGeneratorImpl {

    private static char[] allChars;
    private static char[] numSymbs;
    private static char[] letters;

    /**
     * This will generate a password with the following requirements:
     * - must pass in a length of 8 or greater!
     * - 3 letters in the first 3 characters
     * - 3 numbers or symbols in the next 3 characters
     * - no consecutive characters in a row (e.g. AA or 77)
     * - will loop this method until there are at least 5 distinct characters
     *
     * @param length
     * @return
     */
    public String generatePassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("length < 8: " + length);
        }

        StringBuilder all = new StringBuilder();
        StringBuilder numSym = new StringBuilder();
        StringBuilder letter = new StringBuilder();

        for (char ch = '0'; ch <= '9'; ++ch) {
            all.append(ch);
            numSym.append(ch);
        }
        for (char ch = 'a'; ch <= 'z'; ++ch) {
            all.append(ch);
            letter.append(ch);
        }
        for (char ch = 'A'; ch <= 'Z'; ++ch) {
            all.append(ch);
            letter.append(ch);
        }

        all.append("!$%^*_=+/");
        numSym.append("!$%^*_=+/");

        allChars = all.toString().toCharArray();
        numSymbs = numSym.toString().toCharArray();
        letters = letter.toString().toCharArray();

        Random random = new Random();

        char[] buf = null;

        buf = new char[length];

        // make sure there's numbers in the first 3 characters
        int idx = 0;

        // establish letters in the first 3 characters
        while (idx < 3) {
            if (idx > 0) {
                // check to make sure the previous character is not a duplicate
                char oldChar = buf[idx - 1];
                char newChar = letters[random.nextInt(letters.length)];

                // make sure we don't have any consecutive characters
                while (isConsecutiveCharacter(oldChar, newChar)) {
                    newChar = letters[random.nextInt(letters.length)];
                }

                buf[idx] = newChar;
            } else {
                buf[idx] = letters[random.nextInt(letters.length)];
            }

            idx++;
        }

        // establish symbols and/or numbers
        while (idx < 6) {
            if (idx > 0) {
                // check to make sure the previous character is not a duplicate
                char oldChar = buf[idx - 1];
                char newChar = numSymbs[random.nextInt(numSymbs.length)];

                // make sure we don't have any consecutive characters
                while (isConsecutiveCharacter(oldChar, newChar)) {
                    newChar = numSymbs[random.nextInt(numSymbs.length)];
                }

                buf[idx] = newChar;
            } else {
                buf[idx] = numSymbs[random.nextInt(numSymbs.length)];
            }

            idx++;
        }

        // fill out the rest with whatever!
        while (idx < buf.length) {
            if (idx > 0) {
                // check to make sure the previous character is not a duplicate
                char oldChar = buf[idx - 1];
                char newChar = allChars[random.nextInt(allChars.length)];

                // make sure we don't have any consecutive characters
                while (isConsecutiveCharacter(oldChar, newChar)) {
                    newChar = allChars[random.nextInt(allChars.length)];
                }

                buf[idx] = newChar;
            } else {
                buf[idx] = allChars[random.nextInt(allChars.length)];
            }

            idx++;
        }

        String pw = new String(buf);

        // if we have less than 5 unique characters, run the password generation again!
        // with 14 being the longest AMS accept, this will be very unlikely to happen!
        while (getUniqueCharacterCount(pw) < 5) {
            pw = generatePassword(length);
        }

        return pw;
    }

    /**
     * Use this to check for consecutive characters in a row
     *
     * @param char1
     * @param char2
     * @return
     */
    private boolean isConsecutiveCharacter(char char1, char char2) {
        if (char1 == char2) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the number of unique characters within a String
     *
     * @param input
     * @return
     */
    private int getUniqueCharacterCount(String input) {
        String uniqueCharacters = "";
        for (int i = 0; i < input.length(); i++) {
            if (!uniqueCharacters.contains(String.valueOf(input.charAt(i)))) {
                uniqueCharacters += input.charAt(i);
            }
        }

        return uniqueCharacters.length();
    }
}
