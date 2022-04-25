# Tuner

Discord Multi-Token Musicbot written in Java & Kotlin using [JDA](https://github.com/DV8FromTheWorld/JDA)

**!!! This project is still WIP and is lacking many features !!!**

![img.png](assets/image1.png)

## Features
<ul>
  <li>Supports multiple bot-tokens</li>
  <li>Only one bot will create the commands per guild</li>
  <li>Easily extendable: Not enough bots? Add another one in the config and invite it!</li>
  <li>Easy to use: Just hop in a channel and use the /play command, if no bot is currently playing music, an available bot will join your channel, otherwise it will return an error, if no other bot is available.</li>
</ul>

![img.png](assets/image2.png)

### Spotify Support

Tuner can play spotify links! Just provide a spotify application client id and secret in the `config.yml` file that gets created after first starting the bot, and you should be good to go!

![img.png](assets/image3.png)

### Building

<ol>
  <li>Clone repository: `git clone https://github.com/Xirado/Tuner`</li>
  <li>Go into working directory: `cd Tuner`</li>
  <li>Compile: `./gradlew shadowJar` on Linux/MacOS or `.\gradlew shadowJar` on Windows</li>
  <li>Go into output directory: `cd build/libs`</li>
  <li>Run: `java -jar Tuner-VERSION-all.jar` (Replace VERSION with whatever version it has)</li>
</ol>

 
