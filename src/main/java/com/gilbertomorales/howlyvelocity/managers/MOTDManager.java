package com.gilbertomorales.howlyvelocity.managers;

import com.gilbertomorales.howlyvelocity.comandos.Cores;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Path;

public class MOTDManager {

    private final Path dataDirectory;
    private final File motdFile;
    private final Gson gson;

    private String defaultFirstLine = "&6&lHowlyMC&6.com &7[1.8 - 1.21]";
    private String defaultSecondLine = "&ewww.howlymc.com";
    private String currentSecondLine;
    private boolean maintenance = false;
    private String maintenanceFirstLine = "&c&lHowlyMC&c.com &7[Manutenção]";
    private String maintenanceSecondLine = "&cSaiba mais acessando &ndiscord.gg/howly&c.";

    public MOTDManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.motdFile = new File(dataDirectory.toFile(), "motd.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.currentSecondLine = defaultSecondLine;
        loadMotd();
    }

    public void loadMotd() {
        try {
            if (!motdFile.exists()) {
                saveMotd();
                return;
            }

            try (Reader reader = new FileReader(motdFile)) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                
                if (json.has("secondLine")) {
                    currentSecondLine = json.get("secondLine").getAsString();
                }
                
                if (json.has("maintenance")) {
                    maintenance = json.get("maintenance").getAsBoolean();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveMotd() {
        try {
            if (!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdirs();
            }

            JsonObject json = new JsonObject();
            json.addProperty("secondLine", currentSecondLine);
            json.addProperty("maintenance", maintenance);

            try (Writer writer = new FileWriter(motdFile)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFirstLine() {
        return maintenance ? maintenanceFirstLine : defaultFirstLine;
    }

    public String getSecondLine() {
        return maintenance ? maintenanceSecondLine : currentSecondLine;
    }

    public String getFullMotd() {
        return Cores.colorir(getFirstLine() + "\n" + getSecondLine());
    }

    public void setSecondLine(String secondLine) {
        this.currentSecondLine = secondLine;
        saveMotd();
    }

    public void resetToDefault() {
        this.currentSecondLine = defaultSecondLine;
        saveMotd();
    }

    public boolean isDefault() {
        return currentSecondLine.equals(defaultSecondLine);
    }

    public boolean isInMaintenance() {
        return maintenance;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
        saveMotd();
    }

    public String getMaintenanceKickMessage() {
        return "§c§lHOWLY\n\n§cO Howly está indisponível no momento.\n§cMas fique tranquilo, pois retornaremos em breve!\n\n§cVocê encontrará mais informações sobre essa manutenção em §ndiscord.gg/howly§c.";
    }
}
