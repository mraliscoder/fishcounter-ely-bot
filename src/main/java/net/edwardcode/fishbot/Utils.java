package net.edwardcode.fishbot;

public class Utils {
    public static int validateMessage(String message) {
        // 1.1. Check if message ends with fish
        if (!message.endsWith(" \uD83D\uDC1F")) {
            return -1;
        }
        // 1.2. Check if message starts with numbers
        int currentNum;
        try {
            currentNum = Integer.parseInt(message.replace("\uD83D\uDC1F", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
        return currentNum;
    }
}
