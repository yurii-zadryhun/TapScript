.PHONY: android-debug android-test android-release

android-debug:
	$(MAKE) -C apps/mobile debug

android-test:
	$(MAKE) -C apps/mobile test

android-release:
	$(MAKE) -C apps/mobile release
