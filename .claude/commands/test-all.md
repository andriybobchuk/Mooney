---
description: Run all tests with coverage report
---

# Run All Tests

Executes all unit tests and generates a coverage report.

```bash
echo "🧪 Running all tests..."

# Run tests
./gradlew test

# Generate coverage report if tests pass
if [ $? -eq 0 ]; then
    echo "📊 Generating coverage report..."
    ./gradlew koverHtmlReport
    echo "✅ Tests passed! Coverage report: build/reports/kover/html/index.html"
else
    echo "❌ Some tests failed - fix them before proceeding"
fi
```