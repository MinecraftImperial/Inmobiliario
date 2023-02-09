package ar.net.imperial.inmobiliario.util;


/*
 * Copyright (C) 2012
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class ConfigAccessor {
    private final JavaPlugin plugin;
    private final String fileName;
    private File file;
    private FileConfiguration configuration;

    public ConfigAccessor(JavaPlugin plugin, String fileName){
        this.plugin = plugin;
        this.fileName = fileName;
    }
    public void saveDefault() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), fileName+".yml");
        }
        if (!file.exists()) {
            plugin.saveResource(fileName+".yml", false);
        }
    }

    public FileConfiguration get() {
        if (configuration == null) {
            reload();
        }
        return configuration;
    }

    public void reload(){
        if (file == null) {
            file = new File(plugin.getDataFolder(), fileName+".yml");
        }
        configuration = YamlConfiguration.loadConfiguration(file);

        // Look for defaults in the jar
        Reader defConfigStream;
        @Nullable InputStream res = plugin.getResource(fileName+".yml");
        assert res != null;
        defConfigStream = new InputStreamReader(res, StandardCharsets.UTF_8);
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
        configuration.setDefaults(defConfig);
    }

    @SuppressWarnings("unused")
    public void save() {
        if (configuration == null || file == null) {
            return;
        }
        try {
            get().save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + file, ex);
        }
    }

}