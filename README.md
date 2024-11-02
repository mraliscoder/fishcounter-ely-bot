# FishCounter
Bot written special for Elytrium Discord community

### Requirements
* Java 17 or higher
* MariaDB or MySQL database

### Installation
1. Download JAR file from releases or build project on your computer
2. Copy `config.example.json` into `config.json`, file must be in the same directory with jar file
3. Fill all the fields of `config.json`
4. Create required tables in database using `u1_fish.sql`
5. Run: `java -jar fishbot.jar`

### Config
```json
{
  "mysql": {
    "host": "****", // IP for database, normally localhost
    "user": "u1_fish", // username of database user
    "pass": "****", // password of database user
    "name": "u1_fish" // database name
  },
  "discordBotToken": "****", // discord bot token obtained from discord.com/developers/applications
  "guildId": "1302051463090012331", // your discord server id
  "channelId": "1302051945225392230", // channel id where people are counting fishes
  "auditChannelId": "1302054041093931008", // bot will post notification messages here
  "startLogAfter": 1730508524 // ignore all messages before this time
}
```


:warning: **I have not tested this bot with >= 100 messages (discord history limit)!**

### Copyright notice
Created by [edwardcode](https://edwardcode.net) \
MIT License (see LICENSE)