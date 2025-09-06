# Debugging Summary: Participant Information Not Found in Cache

## Issue Description
When users requested the leaderboard (`+топ` command), the system was showing user IDs instead of usernames. Logs showed that participants were being looked up but not found in the Apache Ignite cache:
```
Участник с ID '256686859583750147' не найден в Apache Ignite
Информация об участнике 256686859583750147 не найдена
```

## Root Cause Analysis
1. **Initialization Order Issue**: In DiscordService, the DiscordMessageListener was being created before StatisticsService was properly initialized with UserService, causing userService reference in StatisticsService to be null during command processing.

2. **Cache Population Issue**: Even when participants were added to challenges by administrators, their information was not being properly saved or retrieved from the Apache Ignite cache.

## Fixes Implemented

### 1. Fixed Initialization Order in DiscordService
**File**: `src/main/java/com/discord/challengebot/service/DiscordService.java`
**Change**: Moved the initialization of StatisticsService dependencies before creating DiscordMessageListener:
```java
// Устанавливаем ссылку на DiscordService в StatisticsService ДО создания DiscordMessageListener
statisticsService.setDiscordService(this);
statisticsService.setUserService(userService);

jda = JDABuilder.createDefault(discordConfig.getToken())
        .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
        .addEventListeners(new DiscordMessageListener(this, discordConfig, challengeService, userService, statisticsService))
        .build();
```

### 2. Enhanced Logging for Debugging
Added comprehensive logging to track the flow of participant information:

**File**: `src/main/java/com/discord/challengebot/service/StatisticsService.java`
- Added detailed debug logging in `formatLeaderboardForDiscord` method to track:
  - User ID being processed
  - Whether UserService is available
  - Whether participant is found in cache
  - Attempts to retrieve username via Discord API
  - Cache update operations

**File**: `src/main/java/com/discord/challengebot/service/UserService.java`
- Enhanced logging in `getParticipant` method to show:
  - When participant lookup is attempted
  - Whether participant is found or not
  - Participant username when found
- Enhanced logging in `registerForChallenge` method to show:
  - When a new participant is created
  - When an existing participant is updated
  - When challenges are added to participant
  - When participant information is saved
- Enhanced logging in `getRegisteredChallenges` method to show:
  - When registered challenges are being retrieved for a user
  - How many challenges are found

**File**: `src/main/java/com/discord/challengebot/service/DataStorageService.java`
- Enhanced logging in `getParticipant` method to show:
  - When participant lookup is attempted in Apache Ignite
  - Whether participant is found in cache
  - Additional cache inspection to count total participants
- Enhanced logging in `saveParticipant` method to show:
  - When participant is being saved
  - Participant ID, username, and number of registered challenges
- Enhanced logging in `getAllParticipants` method to show:
  - When all participants are being retrieved
  - How many participants are found
- Enhanced logging in `init` method to show:
  - When caches are being initialized
  - Initial cache contents

**File**: `src/main/java/com/discord/challengebot/service/ChallengeService.java`
- Enhanced logging in `addParticipantWithUsername` method to show:
  - When participants are being added to challenges
  - Registration status in UserService
  - Progress tracking updates

**File**: `src/main/java/com/discord/challengebot/service/DiscordMessageListener.java`
- Enhanced logging in `handleAddParticipantCommand` method to show:
  - When administrator commands are being processed
  - User information retrieval from Discord API
  - Challenge and participant updates

## Expected Behavior After Fixes
1. When administrators add participants to challenges using `+добавить_участника` command, participants should be properly registered in the cache.
2. When users request leaderboards using `+топ` command, the system should be able to retrieve participant information from the cache.
3. If participant information is not in the cache, the system should attempt to retrieve it via Discord API and update the cache.
4. Detailed logs should show the exact flow of participant information processing, making it easier to identify any remaining issues.

## Additional Notes
- The application requires Java 17 or higher to run, but the current environment only has Java 8 installed.
- Apache Ignite has compatibility issues with newer Java versions that require JVM arguments like `--add-opens java.base/java.io=ALL-UNNAMED`.
- Once the proper Java version is installed and JVM arguments are provided, the application should work correctly with the implemented fixes.