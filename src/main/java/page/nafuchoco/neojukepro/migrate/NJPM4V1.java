/*
 * Copyright 2020 NAFU_at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package page.nafuchoco.neojukepro.migrate;

import page.nafuchoco.neojukepro.core.database.NeoGuildSettingsTable;
import page.nafuchoco.neojukepro.core.guild.NeoGuildPlayerOptions;
import page.nafuchoco.neojukepro.core.guild.NeoGuildSettings;
import page.nafuchoco.neojukepro.core.module.NeoModule;

import java.sql.SQLException;
import java.util.Map;


public class NJPM4V1 extends NeoModule {

    @Override
    public void onLoad() {
        getModuleLogger().info("Starting Guild Setting Migration.");
        GuildSettingsTable guildSettingsTable = new GuildSettingsTable(
                getNeoJukePro().getConfig().getBasicConfig().getDatabase().getTablePrefix(),
                getNeoJukePro().getConnector());
        NeoGuildSettingsTable neoGuildSettingsTable = new NeoGuildSettingsTable(
                getNeoJukePro(),
                getNeoJukePro().getConfig().getBasicConfig().getDatabase().getTablePrefix(),
                getNeoJukePro().getConnector());

        try {
            guildSettingsTable.getGuilds().forEach(guild -> {
                getModuleLogger().info("Found Guild: {}", guild);

                try {
                    if (neoGuildSettingsTable.getGuildSettings(guild) != null) {
                        getModuleLogger().info("The settings for this guild already exist.");
                        return;
                    }
                } catch (SQLException e) {
                    getModuleLogger().warn("This guild setting was skipped because an error occurred while checking the existence of the data.");
                    return;
                }

                try {
                    neoGuildSettingsTable.registerGuildSettings(new NeoGuildSettings(
                            getNeoJukePro(),
                            neoGuildSettingsTable,
                            guild,
                            getNeoJukePro().getConfig().getBasicConfig().getPrefix(),
                            false,
                            false,
                            new NeoGuildPlayerOptions(100, NeoGuildPlayerOptions.RepeatMode.NONE, false)
                    ));
                } catch (SQLException e) {
                    getModuleLogger().warn("Failed to fetch data from SQL.", e);
                }

                Map<String, String> settings;
                try {
                    settings = guildSettingsTable.getGuildSettings(guild);
                } catch (SQLException e) {
                    getModuleLogger().warn("Failed to fetch data from SQL.", e);
                    return;
                }

                settings.entrySet().forEach(setting -> {
                    getModuleLogger().info("Found the setting. Key: {}, Value {}", setting.getKey(), setting.getValue());
                    try {
                        switch (setting.getKey()) {
                            case "prefix":
                                neoGuildSettingsTable.updateCommandPrefixSetting(guild, setting.getValue());
                                break;

                            case "robot":
                                neoGuildSettingsTable.updateRobotModeSetting(guild, Boolean.parseBoolean(setting.getValue()));
                                break;

                            case "autoplay":
                                neoGuildSettingsTable.updateJukeboxModeSetting(guild, Boolean.parseBoolean(setting.getValue()));
                                break;

                            case "volume": {
                                NeoGuildSettings guildSettings = neoGuildSettingsTable.getGuildSettings(guild);
                                NeoGuildPlayerOptions playerOptions;
                                if (guildSettings == null)
                                    playerOptions = new NeoGuildPlayerOptions(
                                            100,
                                            NeoGuildPlayerOptions.RepeatMode.NONE,
                                            false);
                                else
                                    playerOptions = guildSettings.getPlayerOptions();
                                NeoGuildPlayerOptions newPlayerOptions = new NeoGuildPlayerOptions(
                                        Integer.parseInt(setting.getValue()),
                                        playerOptions.getRepeatMode() == null ? NeoGuildPlayerOptions.RepeatMode.NONE : playerOptions.getRepeatMode(),
                                        playerOptions.isShuffle());
                                neoGuildSettingsTable.updatePlayerOptions(guild, newPlayerOptions);
                                break;
                            }

                            case "repeat": {
                                NeoGuildSettings guildSettings = neoGuildSettingsTable.getGuildSettings(guild);
                                NeoGuildPlayerOptions playerOptions;
                                if (guildSettings == null)
                                    playerOptions = new NeoGuildPlayerOptions(
                                            100,
                                            NeoGuildPlayerOptions.RepeatMode.NONE,
                                            false);
                                else
                                    playerOptions = guildSettings.getPlayerOptions();
                                NeoGuildPlayerOptions newPlayerOptions = new NeoGuildPlayerOptions(
                                        playerOptions.getVolumeLevel(),
                                        NeoGuildPlayerOptions.RepeatMode.valueOf(setting.getValue()),
                                        playerOptions.isShuffle());
                                neoGuildSettingsTable.updatePlayerOptions(guild, newPlayerOptions);
                                break;
                            }
                        }
                    } catch (SQLException e) {
                        getModuleLogger().warn("Failed to save data to SQL.", e);
                    }
                });
                getModuleLogger().info("Migration of settings for this guild has been completed.: {}", guild);
            });
            getModuleLogger().info("Migration of all guild settings has been completed.\n" +
                    "Please remove this module.");
        } catch (SQLException e1) {
            getModuleLogger().warn("Failed to fetch data from SQL.", e1);
        }
    }

    @Override
    public void onEnable() {
        setEnable(false);
    }
}
