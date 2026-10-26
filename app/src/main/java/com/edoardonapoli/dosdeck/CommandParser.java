package com.edoardonapoli.dosdeck;

import java.util.Locale;

final class CommandParser {
    enum Action {
        EMPTY, HELP, DIR, START, FIND, CLS, HOME, SETTINGS, WALLPAPER,
        ABOUT, DATE, TIME, RELOAD, COLOR, FONT, VER, SYSTEM, UNKNOWN
    }

    static final class Command {
        final Action action;
        final String argument;

        Command(Action action, String argument) {
            this.action = action;
            this.argument = argument;
        }
    }

    Command parse(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty()) {
            return new Command(Action.EMPTY, "");
        }

        String[] parts = trimmed.split("\\s+", 2);
        String verb = parts[0].toUpperCase(Locale.ROOT);
        String argument = parts.length == 2 ? parts[1].trim() : "";

        switch (verb) {
            case "HELP": case "?": return new Command(Action.HELP, argument);
            case "DIR": case "APPS": case "LS": return new Command(Action.DIR, argument);
            case "START": case "RUN": case "OPEN": return new Command(Action.START, argument);
            case "FIND": case "SEARCH": return new Command(Action.FIND, argument);
            case "CLS": case "CLEAR": return new Command(Action.CLS, argument);
            case "HOME": case "DEFAULT": return new Command(Action.HOME, argument);
            case "SETTINGS": case "CONFIG": return new Command(Action.SETTINGS, argument);
            case "WALLPAPER": return new Command(Action.WALLPAPER, argument);
            case "ABOUT": return new Command(Action.ABOUT, argument);
            case "DATE": return new Command(Action.DATE, argument);
            case "TIME": return new Command(Action.TIME, argument);
            case "RELOAD": case "REFRESH": return new Command(Action.RELOAD, argument);
            case "COLOR": case "COLOUR": return new Command(Action.COLOR, argument);
            case "FONT": return new Command(Action.FONT, argument);
            case "VER": case "VERSION": return new Command(Action.VER, argument);
            case "SYSTEM": case "EXIT": return new Command(Action.SYSTEM, argument);
            default: return new Command(Action.UNKNOWN, trimmed);
        }
    }
}
