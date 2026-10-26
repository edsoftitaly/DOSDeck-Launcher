package com.edoardonapoli.dosdeck;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommandParserTest {
    private final CommandParser parser = new CommandParser();

    @Test
    public void parsesAliasesAndArguments() {
        CommandParser.Command command = parser.parse("  run   Calculator  ");
        assertEquals(CommandParser.Action.START, command.action);
        assertEquals("Calculator", command.argument);
    }

    @Test
    public void parsesCaseInsensitively() {
        assertEquals(CommandParser.Action.DIR, parser.parse("aPpS").action);
        assertEquals(CommandParser.Action.CLS, parser.parse("clear").action);
    }

    @Test
    public void returnsUnknownForUnsupportedCommand() {
        CommandParser.Command command = parser.parse("format c:");
        assertEquals(CommandParser.Action.UNKNOWN, command.action);
        assertEquals("format c:", command.argument);
    }

    @Test
    public void handlesBlankInput() {
        assertEquals(CommandParser.Action.EMPTY, parser.parse("   ").action);
    }
}
