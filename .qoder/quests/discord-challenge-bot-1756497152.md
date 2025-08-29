# Discord Challenge Bot Design Document

## 1. Overview

The Discord Challenge Bot is a Spring Boot application that enables users to create and participate in fitness challenges within a Discord server. The bot allows users to track their progress on challenges like "10,000 push-ups by the end of the year" either individually or as a group. The bot responds to specific commands in a designated channel and provides statistics and progress updates.

### Key Features
- Create and manage fitness challenges with individual or group participation
- Track progress through user commands starting with "+"
- Automatic daily progress reports at 7 AM
- Statistics tracking and visualization capabilities
- Configuration through application.yml

## 2. Technology Stack

- **Language**: Java 17 or 21
- **Build Tool**: Maven
- **Framework**: Spring Boot
- **Data Storage**: Apache Ignite 2.17
- **Discord Integration**: Discord API (JDA - Java Discord API)
- **Scheduling**: Spring Scheduling
- **Configuration**: YAML-based configuration

## 3. Architecture

```
graph TD
    A[Discord Client] -->|Messages| B[Discord Bot Application]
    B --> C[Message Handler]
    C --> D[Command Processor]
    D --> E[Challenge Service]
    E --> F[Ignite Data Store]
    D --> G[Statistics Service]
    G --> F
    H[Scheduled Tasks] --> E
    H --> G
```

### Component Description
- **Discord Client**: The Discord platform where users interact with the bot
- **Discord Bot Application**: The main Spring Boot application
- **Message Handler**: Component that listens for messages in the designated channel
- **Command Processor**: Parses and routes commands to appropriate services
- **Challenge Service**: Manages creation, modification, and tracking of challenges
- **Statistics Service**: Generates statistics and progress reports
- **Ignite Data Store**: Apache Ignite cluster for data persistence
- **Scheduled Tasks**: Daily scheduled tasks for progress reporting

## 4. Data Models

### Challenge Model
```
public class Challenge {
    private String id;
    private String name;
    private long targetValue;
    private long currentValue;
    private ChallengeType type; // INDIVIDUAL or GROUP
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Map<String, Long> participantProgress; // userId -> progress
    private boolean active;
    private String description;
    private String unit; // e.g., "push-ups", "km", "minutes"
}
```

### Participant Model
```
public class Participant {
    private String userId;
    private String username;
    private LocalDateTime joinDate;
    private Map<String, Long> challengeProgress; // challengeId -> progress
    private List<String> registeredChallenges; // List of challenge IDs the user is registered for
}
```

### Progress Entry Model
```
public class ProgressEntry {
    private String id;
    private String userId;
    private String challengeId;
    private long amount;
    private LocalDateTime timestamp;
    private String note; // Optional note about the progress
}
```

### Enums
```
public enum ChallengeType {
    INDIVIDUAL,
    GROUP
}
```

## 5. Configuration

The bot will be configured through `application.yml`:

```
# Discord bot configuration
discord:
  token: ${DISCORD_BOT_TOKEN} # Discord bot token from environment variable
  channel: "качал-очка"        # Channel name where bot responds
  guild-id: ${DISCORD_GUILD_ID} # Guild/Server ID
  admin-user-id: ${DISCORD_ADMIN_USER_ID} # User ID with admin privileges

# Challenge configuration
challenges:
  default:
    daily-reminder-time: "07:00" # Time for daily progress reports (24-hour format)
    timezone: "Europe/Moscow"    # Timezone for scheduling
  
# Apache Ignite configuration
ignite:
  client-mode: true
  addresses:
    - "127.0.0.1:10800"  # Ignite server address
  
# Scheduled tasks configuration
scheduled:
  enabled: true
  cron:
    daily-report: "0 0 7 * * ?"  # Every day at 7:00 AM

# Visualization settings
visualization:
  enabled: true
  max-width: 400
  max-height: 300
  format: "png"

# Logging configuration
logging:
  level:
    com.discord.challengebot: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"
```

## 6. Command Reference

### Core Commands
| Command | Description | Example | Permissions |
|---------|-------------|---------|-------------|
| `+<challenge> <amount>` | Add progress to a challenge | `+отжимания 10` | All users |
| `+статистика` | Show statistics for all challenges | `+статистика` | All users |
| `+статистика <challenge>` | Show statistics for specific challenge | `+статистика отжимания` | All users |
| `+помощь` | Show help documentation | `+помощь` | All users |

### Challenge Management Commands
| Command | Description | Example | Permissions |
|---------|-------------|---------|-------------|
| `+новый <name> <target> [end_date] [type]` | Create a new challenge | `+новый отжимания 10000 2024-12-31 групповой` | Admin users |
| `+удалить <name>` | Delete a challenge | `+удалить отжимания` | Admin users |
| `+остановить <name>` | Stop an active challenge | `+остановить отжимания` | Admin users |
| `+продолжить <name>` | Resume a stopped challenge | `+продолжить отжимания` | Admin users |
| `+изменить <name> <new_target>` | Change challenge target | `+изменить отжимания 15000` | Admin users |
| `+регистрация <name>` | Register for a challenge | `+регистрация отжимания` | All users |

### User Commands
| Command | Description | Example | Permissions |
|---------|-------------|---------|-------------|
| `+мои` | Show user's personal challenges | `+мои` | All users |
| `+топ <challenge> [count]` | Show leaderboard for a challenge | `+топ отжимания 10` | All users |
| `+прогресс <challenge>` | Show personal progress for a challenge | `+прогресс отжимания` | All users |
| `+регистрация <name>` | Register for a challenge | `+регистрация отжимания` | All users |

## 7. Business Logic

### Message Processing Flow
```
graph TD
    A[Message Received] --> B{Channel Check}
    B -->|Wrong Channel| C[Ignore]
    B -->|Correct Channel| D{Starts with +}
    D -->|No| C
    D -->|Yes| E[Parse Command]
    E --> F{Valid Command}
    F -->|No| G[Show Error]
    F -->|Yes| H[Execute Command]
    H --> I[Update Data Store]
    I --> J[Generate Response]
    J --> K[Send Response to Channel]
```

### Progress Tracking Logic
When a user sends a message like `+отжимания 10`:
1. **Validation**: Check if message is in the correct channel and starts with "+"
2. **Parsing**: Extract command parts (challenge name: "отжимания", amount: 10)
3. **Challenge Lookup**: Find challenge by name in Ignite cache
4. **Validation**: Verify challenge exists and is active
5. **User Registration Check**: Verify user is registered for the challenge (for individual challenges)
6. **Progress Update**: 
   - Update challenge's `currentValue`
   - Update user's progress in `participantProgress` map
   - Create new ProgressEntry record
7. **Statistics Calculation**:
   - Calculate remaining amount: `targetValue - currentValue`
   - Calculate percentage completed: `(currentValue / targetValue) * 100`
   - Calculate daily target: `(targetValue - currentValue) / daysRemaining`
8. **Response Generation**: Format statistics into Discord-friendly message
9. **Response Delivery**: Send formatted message back to channel

### User Registration Logic
When a user sends a message like `+регистрация отжимания`:
1. **Validation**: Check if message is in the correct channel and starts with "+"
2. **Parsing**: Extract command parts (command: "регистрация", challenge name: "отжимания")
3. **Challenge Lookup**: Find challenge by name in Ignite cache
4. **Validation**: Verify challenge exists and is active
5. **Registration Check**: Verify user is not already registered
6. **Registration Process**: 
   - Add challenge to user's `registeredChallenges` list
   - Initialize user's progress for this challenge
   - Update participant cache
7. **Confirmation**: Send confirmation message to user

### Admin Authorization Logic
When processing admin commands:
1. **User ID Check**: Compare user ID with `admin-user-id` from configuration
2. **Authorization**: Allow or deny access based on ID match
3. **Command Execution**: Execute command if authorized
4. **Audit Logging**: Log admin actions for security monitoring

### Daily Progress Reports Logic
A scheduled task runs daily at 7 AM:
1. **Challenge Retrieval**: Get all active challenges from Ignite cache
2. **Statistics Calculation**: For each challenge, calculate:
   - Total progress made: `currentValue`
   - Remaining amount: `targetValue - currentValue`
   - Days remaining: `endDate - currentDate`
   - Daily average needed: `remainingAmount / daysRemaining`
   - Percentage completed: `(currentValue / targetValue) * 100`
3. **Message Formatting**: Format information within Discord's 2000 character limit
4. **Report Generation**: Create comprehensive report with all active challenges
5. **Delivery**: Send report to the designated channel

## 8. Services

### Challenge Service
- `Challenge createChallenge(String name, long targetValue, LocalDateTime endDate, ChallengeType type, String description, String unit)`: Create a new challenge
- `Challenge addProgress(String challengeName, String userId, String username, long amount)`: Add progress to a challenge
- `Challenge getChallenge(String name)`: Get a specific challenge by name
- `List<Challenge> getAllChallenges()`: Get all challenges
- `ChallengeStats getChallengeStats(String challengeName)`: Get statistics for a challenge
- `Map<String, ChallengeStats> getAllChallengesStats()`: Get statistics for all challenges
- `List<Challenge> getUserChallenges(String userId)`: Get challenges for a specific user
- `boolean deleteChallenge(String challengeName)`: Delete a challenge
- `Challenge updateChallengeStatus(String challengeName, boolean active)`: Activate/deactivate a challenge
- `Challenge updateChallengeTarget(String challengeName, long newTarget)`: Update challenge target value

### User Service
- `boolean registerForChallenge(String userId, String username, String challengeName)`: Register user for a challenge
- `boolean unregisterFromChallenge(String userId, String challengeName)`: Unregister user from a challenge
- `Participant getParticipant(String userId)`: Get participant information
- `List<Challenge> getRegisteredChallenges(String userId)`: Get challenges user is registered for
- `boolean isAdminUser(String userId)`: Check if user has admin privileges

### Statistics Service
- `ChallengeStats calculateStats(Challenge challenge)`: Calculate comprehensive statistics for a challenge
- `long calculateRemaining(Challenge challenge)`: Calculate remaining amount for challenge
- `double calculateDailyTarget(Challenge challenge)`: Calculate daily target to finish on time
- `double calculatePercentage(Challenge challenge)`: Calculate completion percentage
- `String generateProgressReport(Challenge challenge)`: Generate formatted progress report
- `List<LeaderboardEntry> generateLeaderboard(Challenge challenge, int limit)`: Generate leaderboard for challenge
- `String formatReportForDiscord(ChallengeStats stats)`: Format statistics for Discord message limits

### Discord Service
- `void sendMessage(String channelId, String message)`: Send message to Discord channel
- `void sendMessageWithVisualization(String channelId, String message, byte[] image)`: Send message with image attachment
- `Command parseCommand(String messageContent)`: Parse incoming Discord message into command
- `String generateHelpMessage()`: Generate help documentation
- `void sendDailyReport()`: Send daily progress report
- `String formatChallengeStats(ChallengeStats stats)`: Format challenge statistics for Discord
- `boolean isAuthorizedUser(String userId, String command)`: Check if user is authorized for command

## 9. Scheduled Tasks

### Daily Progress Report
```java
@Scheduled(cron = "${scheduled.cron.daily-report}")
public void sendDailyProgressReports() {
    // Implementation details:
    // 1. Retrieve all active challenges from Ignite
    // 2. Calculate statistics for each challenge
    // 3. Format comprehensive report within Discord limits
    // 4. Send to designated channel
}
```

### Challenge Completion Check
```java
@Scheduled(cron = "0 0 * * * ?") // Every hour
public void checkChallengeCompletions() {
    // Implementation details:
    // 1. Check for challenges that have reached their target
    // 2. Send completion notifications
    // 3. Mark challenges as completed
}
```

### Database Cleanup
```java
@Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
public void cleanupOldData() {
    // Implementation details:
    // 1. Remove old progress entries based on retention policy
    // 2. Archive completed challenges
}
```

## 10. Data Storage with Apache Ignite

### Ignite Configuration
- Challenge cache for storing challenge data with expiration policies
- Participant cache for storing user data
- Progress entry cache for detailed tracking
- Indexes on frequently queried fields (challenge name, user ID, etc.)
- Persistence enabled for data durability

### Cache Structure
```java
// Challenge cache configuration
@Bean
public IgniteCache<String, Challenge> challengeCache(Ignite ignite) {
    CacheConfiguration<String, Challenge> cfg = new CacheConfiguration<>("challenges");
    cfg.setIndexedTypes(String.class, Challenge.class);
    cfg.setBackups(1);
    return ignite.getOrCreateCache(cfg);
}

// Participant cache configuration
@Bean
public IgniteCache<String, Participant> participantCache(Ignite ignite) {
    CacheConfiguration<String, Participant> cfg = new CacheConfiguration<>("participants");
    cfg.setIndexedTypes(String.class, Participant.class);
    cfg.setBackups(1);
    return ignite.getOrCreateCache(cfg);
}

// Progress entry cache configuration
@Bean
public IgniteCache<String, ProgressEntry> progressEntryCache(Ignite ignite) {
    CacheConfiguration<String, ProgressEntry> cfg = new CacheConfiguration<>("progress");
    cfg.setIndexedTypes(String.class, ProgressEntry.class);
    cfg.setBackups(1);
    return ignite.getOrCreateCache(cfg);
}
```

### Data Access Patterns
- Primary key lookups for challenge and participant data
- SQL queries for complex statistics and reporting
- Scan queries for scheduled tasks
- Atomic updates for progress tracking

## 11. Logging Configuration

The application uses SLF4J with Logback for logging. All logging is configured through the `application.yml` file.

### Log Levels
- **INFO**: General application flow and important events
- **WARN**: Potentially harmful situations
- **ERROR**: Error events that might still allow the application to continue running

### Log Format
```
%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n
```

### Key Logging Points
- Command processing start and completion
- Challenge creation, modification, and deletion
- User registration for challenges
- Progress updates
- Scheduled task execution
- Error conditions and exceptions
- Discord API interactions
- Cache operations with Ignite

### Example Log Messages
```java
// Command processing
logger.info("Processing command '{}' from user '{}'", command.getName(), userId);

// Challenge operations
logger.info("Created new challenge '{}' with target {}", challenge.getName(), challenge.getTargetValue());

// Error handling
logger.error("Failed to update progress for challenge '{}': {}", challengeName, exception.getMessage(), exception);
```

## 12. Testing Strategy

### Unit Tests
- Challenge service logic for creating and updating challenges
- Statistics calculations for accuracy
- Command parsing for various input formats
- Data validation for user inputs
- Progress tracking algorithms
- User registration functionality

### Integration Tests
- Discord message processing with mock JDA
- Cache operations with embedded Ignite instance
- Scheduled task execution with mocked clocks
- Database persistence and retrieval
- API endpoint testing
- User registration and challenge participation flows

### Test Libraries and Frameworks
- JUnit 5 for test structure
- Mockito for mocking dependencies (Discord API, Ignite)
- Testcontainers for integration testing with real Ignite instance
- Awaitility for testing asynchronous operations
- AssertJ for fluent assertions

### Test Categories
```java
// Unit test example
@Test
void shouldAddProgressToChallenge() {
    // Given
    Challenge challenge = new Challenge("отжимания", 10000, ChallengeType.GROUP);
    when(challengeService.getChallenge("отжимания")).thenReturn(challenge);
    
    // When
    Challenge updated = challengeService.addProgress("отжимания", "user123", "username", 10);
    
    // Then
    assertEquals(10, updated.getCurrentValue());
    verify(challengeCache).put(anyString(), any(Challenge.class));
}

// Integration test example
@Test
void shouldPersistChallengeInIgnite() {
    // Given
    Challenge challenge = new Challenge("отжимания", 10000, ChallengeType.GROUP);
    
    // When
    challengeService.createChallenge(challenge);
    Challenge retrieved = challengeService.getChallenge("отжимания");
    
    // Then
    assertThat(retrieved).usingRecursiveComparison().isEqualTo(challenge);
}

// Registration test
@Test
void shouldRegisterUserForChallenge() {
    // Given
    String userId = "user123";
    String challengeId = "отжимания";
    
    // When
    boolean result = userService.registerForChallenge(userId, challengeId);
    
    // Then
    assertTrue(result);
    verify(participantCache).put(anyString(), argThat(participant -> 
        participant.getRegisteredChallenges().contains(challengeId)));
}
```

## 13. Additional Features

### Visualization
- Generate progress charts as PNG images using JFreeChart
- Create leaderboard visualizations
- Text-based progress bars for simple displays
- Handle Discord attachment size limits (8MB for regular servers)
- Cache generated images to reduce processing overhead

### Notifications
- Milestone achievement notifications (25%, 50%, 75%, 90% completion)
- Challenge completion alerts for individuals and groups
- Low progress warnings when falling behind daily targets
- Personal progress summaries on request
- Custom reminder notifications

### Administration
- Challenge management commands (start, stop, modify, delete)
- User management (ban, reset progress)
- Configuration reload without restart
- Backup and restore functionality
- Detailed logging and monitoring

### User Experience Enhancements
- Full Russian language support for all commands, responses, and comments in code
- Simplified command structure using spaces instead of underscores
- Customizable challenge units (push-ups, km, minutes, etc.)
- Challenge categories and tagging
- Personal goal setting alongside group challenges
- Progress history and trends
- Achievement badges and rewards system

## 14. Error Handling

### Command Processing Errors
- Invalid command format with helpful usage examples
- Challenge not found scenarios with suggestions
- Invalid number formats with correction guidance
- User not registered in system (auto-register on first command)
- Insufficient permissions for administrative commands

### Data Persistence Errors
- Cache connection failures with retry mechanisms
- Data serialization/deserialization issues
- Transaction rollback scenarios
- Backup and recovery procedures

### Discord API Errors
- Rate limiting with exponential backoff
- Network communication failures
- Invalid channel or guild identification
- Permission errors for message sending

### Error Response Patterns
```
// Error handling in command processor
try {
    Challenge updated = challengeService.addProgress(challengeName, userId, username, amount);
    return generateSuccessResponse(updated);
} catch (ChallengeNotFoundException e) {
    return "Challenge not found. Available challenges: " + getAllChallengeNames();
} catch (IllegalArgumentException e) {
    return "Invalid amount. Please provide a positive number.";
} catch (DataAccessException e) {
    logger.error("Database error occurred", e);
    return "Sorry, we encountered a technical issue. Please try again later.";
}
```

## 15. Security Considerations

### Credential Management
- Secure storage of Discord token using environment variables
- Encryption of sensitive configuration parameters
- Regular credential rotation procedures
- Minimal privilege principle for bot permissions

### Input Validation
- Sanitization of all user inputs to prevent injection attacks
- Length limits on command parameters
- Character set restrictions for challenge names
- Rate limiting to prevent abuse

### Access Control
- Role-based access control for administrative commands
- User authentication through Discord OAuth
- Challenge ownership verification for modification rights
- Audit logging of all administrative actions

### Data Protection
- Encryption at rest for sensitive user data
- Secure communication with Ignite cluster
- Regular backups with encryption
- Data retention and deletion policies

### Network Security
- Firewall rules for Ignite cluster access
- Secure communication protocols (TLS)
- Regular security updates for all dependencies
- Vulnerability scanning in CI/CD pipeline

## 16. Project Structure and Dependencies

### Maven Dependencies
```
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Discord API -->
    <dependency>
        <groupId>net.dv8tion</groupId>
        <artifactId>JDA</artifactId>
        <version>5.0.0-beta.12</version>
    </dependency>
    
    <!-- Apache Ignite -->
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-core</artifactId>
        <version>2.17.0</version>
    </dependency>
    
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-spring</artifactId>
        <version>2.17.0</version>
    </dependency>
    
    <!-- Visualization -->
    <dependency>
        <groupId>org.jfree</groupId>
        <artifactId>jfreechart</artifactId>
        <version>1.5.3</version>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.18.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── discord/
│   │           └── challengebot/
│   │               ├── ChallengeBotApplication.java
│   │               ├── configuration/
│   │               │   ├── DiscordConfig.java
│   │               │   ├── IgniteConfig.java
│   │               │   └── ScheduleConfig.java
│   │               ├── controller/
│   │               │   └── DiscordMessageController.java
│   │               ├── service/
│   │               │   ├── ChallengeService.java
│   │               │   ├── StatisticsService.java
│   │               │   ├── DiscordService.java
│   │               │   └── VisualizationService.java
│   │               ├── model/
│   │               │   ├── Challenge.java
│   │               │   ├── Participant.java
│   │               │   ├── ProgressEntry.java
│   │               │   └── ChallengeType.java
│   │               └── scheduler/
│   │                   └── DailyReportScheduler.java
│   └── resources/
│       ├── application.yml
│       └── messages.properties
└── test/
    └── java/
        └── com/
            └── discord/
                └── challengebot/
                    ├── service/
                    │   ├── ChallengeServiceTest.java
                    │   └── StatisticsServiceTest.java
                    └── integration/
                        ├── DiscordServiceIntegrationTest.java
                        └── IgniteIntegrationTest.java
```

### Main Application Class
```java
@SpringBootApplication
@EnableScheduling
public class ChallengeBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChallengeBotApplication.class, args);
    }
}
```


## 17. Logging Configuration

The application uses SLF4J with Logback for logging. All logging is configured through the `application.yml` file.

### Log Levels
- **INFO**: General application flow and important events
- **WARN**: Potentially harmful situations
- **ERROR**: Error events that might still allow the application to continue running

### Log Format
```
%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n
```

### Key Logging Points
- Command processing start and completion
- Challenge creation, modification, and deletion
- User registration for challenges
- Progress updates
- Scheduled task execution
- Error conditions and exceptions
- Discord API interactions
- Cache operations with Ignite

### Example Log Messages
```java
// Command processing
logger.info("Processing command '{}' from user '{}'", command.getName(), userId);

// Challenge operations
logger.info("Created new challenge '{}' with target {}", challenge.getName(), challenge.getTargetValue());

// Error handling
logger.error("Failed to update progress for challenge '{}': {}", challengeName, exception.getMessage(), exception);
```

## 18. Testing Strategy

### Unit Tests
- Challenge service logic for creating and updating challenges
- Statistics calculations for accuracy
- Command parsing for various input formats
- Data validation for user inputs
- Progress tracking algorithms
- User registration functionality

### Integration Tests
- Discord message processing with mock JDA
- Cache operations with embedded Ignite instance
- Scheduled task execution with mocked clocks
- Database persistence and retrieval
- API endpoint testing
- User registration and challenge participation flows

### Test Libraries and Frameworks
- JUnit 5 for test structure
- Mockito for mocking dependencies (Discord API, Ignite)
- Testcontainers for integration testing with real Ignite instance
- Awaitility for testing asynchronous operations
- AssertJ for fluent assertions

### Test Categories
```java
// Unit test example
@Test
void shouldAddProgressToChallenge() {
    // Given
    Challenge challenge = new Challenge("отжимания", 10000, ChallengeType.GROUP);
    when(challengeService.getChallenge("отжимания")).thenReturn(challenge);
    
    // When
    Challenge updated = challengeService.addProgress("отжимания", "user123", "username", 10);
    
    // Then
    assertEquals(10, updated.getCurrentValue());
    verify(challengeCache).put(anyString(), any(Challenge.class));
}

// Integration test example
@Test
void shouldPersistChallengeInIgnite() {
    // Given
    Challenge challenge = new Challenge("отжимания", 10000, ChallengeType.GROUP);
    
    // When
    challengeService.createChallenge(challenge);
    Challenge retrieved = challengeService.getChallenge("отжимания");
    
    // Then
    assertThat(retrieved).usingRecursiveComparison().isEqualTo(challenge);
}

// Registration test
@Test
void shouldRegisterUserForChallenge() {
    // Given
    String userId = "user123";
    String challengeId = "отжимания";
    
    // When
    boolean result = userService.registerForChallenge(userId, challengeId);
    
    // Then
    assertTrue(result);
    verify(participantCache).put(anyString(), argThat(participant -> 
        participant.getRegisteredChallenges().contains(challengeId)));
}
```

