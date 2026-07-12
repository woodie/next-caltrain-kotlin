.PHONY: lint test check

# lint and test are always verbose. check is terse: suppress everything on
# success, dump the full log on any failure -- matching the intent (not
# necessarily the literal dots) of Go's/Ruby's/Swift's own lint/test/check
# split in this account (see humane/humane-ruby/humane-swift's Makefiles).

lint:
	ktlint

# Verbose on purpose -- app/build.gradle.kts registers a custom Gradle
# TestListener that renders Kotest's describe/it tree with checkmarks, the
# Kotlin equivalent of Ruby's `rspec -fd` / Swift's `swift test | xctidy`.
test:
	./test.sh

# Terser than `test` on purpose: Gradle's own test task has no quiet mode to
# pair with the TestListener above, so this just suppresses output on
# success and dumps the full log on failure, guaranteeing errors are never
# hidden regardless of the build's exact output.
check: lint
	@LOG=$$(mktemp); \
	if ./test.sh > "$$LOG" 2>&1; then \
		echo "PASS"; \
	else \
		cat "$$LOG"; \
		rm -f "$$LOG"; \
		exit 1; \
	fi; \
	rm -f "$$LOG"
