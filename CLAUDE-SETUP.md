# Claude Code Setup Guide for Mooney

## 🚀 Quick Start

### Using Claude Code with Mooney

```bash
# Navigate to project
cd ~/dev/Mooney

# Start Claude Code
claude

# Check Figma connection
/mcp

# See available commands
/commands
```

## 📋 Available Commands

| Command | Description | Usage |
|---------|-------------|-------|
| `/review` | Code review for changes | `/review` or `/review path/to/file` |
| `/quick-build` | Build Android app | `/quick-build debug` |
| `/test-all` | Run all tests | `/test-all` |
| `/new-feature` | Scaffold new feature | `/new-feature Transaction` |

## 🤖 Specialized Agents

### test-writer
Writes comprehensive unit tests for ViewModels and Use Cases
```
@test-writer write tests for AssetsViewModel
```

### ui-builder  
Creates Compose UI screens from Figma designs
```
@ui-builder create a transaction details screen
```

## 🎯 Skills

### database
Automatically activated when working with Room entities or migrations
- Handles schema updates
- Creates migrations
- Manages type converters

### review
Code review with focus on KMP best practices
```
/review
```

## 🎨 Figma Integration

The Figma MCP server is configured to access your design system:
- Mobile Apps Prototyping Kit
- Design tokens mapped to code
- Component generation from designs

To generate screens from Figma:
```
Generate screen from: [FIGMA_URL]
Use Material3 components and follow CLAUDE.md patterns
```

## ⚡ Pro Tips

### 1. Complex Features
For multi-step implementations:
```
/plan
[Describe the feature]
```

### 2. Better Quality
Switch to Opus for complex tasks:
```
/model claude-opus-4-6
```

### 3. Clean Context
Between major tasks:
```
/clear
```

### 4. Check Git Status
```
git status
```
Always review before committing!

## 🔧 Configuration

### Hooks
The setup includes smart hooks that:
- Warn about code quality issues
- Suggest next steps after file creation
- Block destructive git commands
- Remind about test updates

### Permissions
- ✅ Gradle builds allowed
- ✅ Git operations (with safety checks)
- ✅ Web search and documentation lookup
- ❌ Secrets and credentials protected
- ⚠️ Destructive commands require confirmation

## 📁 Project Structure

```
.claude/
├── settings.json          # Main configuration
├── settings.local.json    # Local overrides (gitignored)
├── skills/
│   ├── review/           # Code review skill
│   └── database/         # Database operations
├── agents/
│   ├── test-writer.md    # Test generation agent
│   └── ui-builder.md     # UI creation agent
└── commands/
    ├── quick-build.md    # Build command
    ├── test-all.md       # Test runner
    └── new-feature.md    # Feature scaffolding
```

## 🚦 Workflow

### Starting a New Feature
1. Use `/new-feature FeatureName` to scaffold
2. Implement with guidance from CLAUDE.md
3. Use `@test-writer` for test generation
4. Run `/test-all` to verify
5. Use `/review` before committing

### Working with Figma
1. Get design URL from Figma
2. Use `@ui-builder` with the URL
3. Follow Material3 patterns
4. Add @Preview composables
5. Test dark mode

### Database Changes
1. Modify entity in `core/data/database/Entity.kt`
2. The database skill auto-activates
3. Follow migration guidance
4. Increment version in AppDatabase
5. Test migration with sample data

## 🐛 Troubleshooting

### Figma Not Connected
```
/mcp
```
Then authenticate in browser

### Tests Failing
```
./gradlew test --info
```
Check detailed output

### Build Issues
```
./gradlew clean
./gradlew build --refresh-dependencies
```

## 📚 Key Resources

- **Architecture**: See CLAUDE.md
- **Design System**: Figma Mobile Apps Kit
- **Testing**: MockK + Coroutines Test
- **Database**: Room with SQLite

## 💡 Best Practices

1. **Always follow MVVM** - ViewModel → UseCase → Repository
2. **State management** - Use `update { }` not direct assignment
3. **Error handling** - Never swallow CancellationException
4. **Testing** - Every business logic needs tests
5. **Commits** - Single-line, descriptive, no AI mentions

---

**Remember**: This is a finance app handling real money. Quality and correctness are paramount!
