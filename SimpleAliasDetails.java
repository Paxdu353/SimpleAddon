package fr.paxdu353.bettersimpleallias.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleAliasDetails {
    private static final String[] LABELS = new String[] {
            "Name",
            "Type",
            "Description",
            "Executor",
            "Executable As Console",
            "Message When Cooldown Enabled",
            "Cooldown Enabled",
            "Global Cooldown",
            "Price",
            "Allow Buy",
            "Commands",
            "Subtitle",
            "Permission Enabled",
            "Permitted Groups Enabled"
    };

    private final Map<String, String> fields;
    private final List<String> permissionNodes;

    private SimpleAliasDetails(Map<String, String> fields, List<String> permissionNodes) {
        this.fields = fields;
        this.permissionNodes = permissionNodes;
    }

    static SimpleAliasDetails parse(String rawMessage) {
        String clean = stripColors(rawMessage).replace('\r', ' ').replace('\n', ' ');
        clean = clean.replaceAll("\\s+", " ").trim();

        if (!clean.startsWith("[SimpleAlias]") || clean.indexOf("Detailed information about the alias") < 0) {
            return null;
        }

        Map<String, Integer> positions = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < LABELS.length; i++) {
            String label = LABELS[i];
            int position = clean.indexOf(label + ":");
            if (position >= 0) {
                positions.put(label, Integer.valueOf(position));
            }
        }

        if (!positions.containsKey("Name") || !positions.containsKey("Commands")) {
            return null;
        }

        Map<String, String> fields = new LinkedHashMap<String, String>();
        for (int i = 0; i < LABELS.length; i++) {
            String label = LABELS[i];
            Integer startPosition = positions.get(label);
            if (startPosition == null) {
                continue;
            }

            int valueStart = startPosition.intValue() + label.length() + 1;
            int valueEnd = clean.length();
            for (int j = 0; j < LABELS.length; j++) {
                if (i == j) {
                    continue;
                }

                Integer next = positions.get(LABELS[j]);
                if (next != null && next.intValue() > valueStart && next.intValue() < valueEnd) {
                    valueEnd = next.intValue();
                }
            }

            fields.put(label, cleanFieldValue(label, clean.substring(valueStart, valueEnd)));
        }

        List<String> permissions = extractPermissionNodes(clean);
        return new SimpleAliasDetails(fields, permissions);
    }

    List<String> toChatLines() {
        List<String> lines = new ArrayList<String>();
        String alias = value("Name");

        lines.add(color('2') + "[SimpleAlias] " + color('e') + "Details de " + color('a') + alias);
        lines.add(color('8') + "------------------------------");
        lines.add(color('b') + "Parametres:");
        addField(lines, "Nom", value("Name"));
        addField(lines, "Type", value("Type"));
        addField(lines, "Description", value("Description"));
        addField(lines, "Executeur", value("Executor"));
        addField(lines, "Console", value("Executable As Console"));
        addField(lines, "Cooldown active", value("Cooldown Enabled"));
        addField(lines, "Message cooldown", value("Message When Cooldown Enabled"));
        addField(lines, "Cooldown global", value("Global Cooldown"));
        addField(lines, "Prix", value("Price"));
        addField(lines, "Achat autorise", value("Allow Buy"));
        addField(lines, "Sous-titre", value("Subtitle"));
        addField(lines, "Permission active", value("Permission Enabled"));
        addPermissionLines(lines);
        addField(lines, "Groupes actives", value("Permitted Groups Enabled"));

        lines.add(color('b') + "Commandes:");
        List<String> commands = commands();
        if (commands.isEmpty()) {
            lines.add(color('8') + " - " + color('7') + "aucune");
        } else {
            for (int i = 0; i < commands.size(); i++) {
                lines.add(color('8') + " " + (i + 1) + ". " + color('f') + commands.get(i));
            }
        }

        return lines;
    }

    private void addPermissionLines(List<String> lines) {
        if (permissionNodes.isEmpty()) {
            return;
        }

        if (permissionNodes.size() == 1) {
            addField(lines, "Permission", permissionNodes.get(0));
            return;
        }

        lines.add(color('8') + " - " + color('7') + "Permissions:");
        for (int i = 0; i < permissionNodes.size(); i++) {
            lines.add(color('8') + "   " + (i + 1) + ". " + color('f') + permissionNodes.get(i));
        }
    }

    private void addField(List<String> lines, String label, String value) {
        if (value.length() == 0) {
            return;
        }

        lines.add(color('8') + " - " + color('7') + label + ": " + valueColor(value) + value);
    }

    private List<String> commands() {
        String raw = value("Commands");
        if (raw.length() == 0) {
            return Collections.emptyList();
        }

        String[] split = raw.split("#");
        List<String> commands = new ArrayList<String>();
        for (int i = 0; i < split.length; i++) {
            String command = cleanValue(split[i]);
            if (command.length() > 0) {
                commands.add(command);
            }
        }
        return commands;
    }

    private String value(String key) {
        String value = fields.get(key);
        return value == null ? "" : value;
    }

    private static List<String> extractPermissionNodes(String clean) {
        List<String> permissions = new ArrayList<String>();
        int index = 0;
        while (true) {
            int found = clean.indexOf("SimpleAlias.alias.", index);
            if (found < 0) {
                break;
            }

            int end = clean.indexOf(' ', found);
            if (end < 0) {
                end = clean.length();
            }

            String permission = cleanValue(clean.substring(found, end));
            if (permission.length() > 0 && !permissions.contains(permission)) {
                permissions.add(permission);
            }
            index = end;
        }
        return permissions;
    }

    private static String cleanValue(String value) {
        String clean = value.trim();
        while (clean.startsWith("=")) {
            clean = clean.substring(1).trim();
        }
        while (clean.endsWith("=")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        return clean;
    }

    private static String cleanFieldValue(String label, String value) {
        String clean = cleanValue(value);
        if ("Permission Enabled".equals(label)) {
            int permissionStart = clean.indexOf("SimpleAlias.alias.");
            if (permissionStart >= 0) {
                clean = clean.substring(0, permissionStart).trim();
            }
        }
        return cleanValue(clean);
    }

    private static String stripColors(String value) {
        return value.replaceAll("(?i)\\u00A7[0-9A-FK-OR]", "");
    }

    private static String valueColor(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return color('a');
        }
        if ("false".equalsIgnoreCase(value)) {
            return color('c');
        }
        if ("0".equals(value) || "0.0".equals(value) || "0 seconds".equalsIgnoreCase(value)) {
            return color('a');
        }
        return color('f');
    }

    private static String color(char code) {
        return "\u00A7" + code;
    }
}
