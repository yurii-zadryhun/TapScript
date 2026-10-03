.PHONY: debug test install release clean

debug:
	./gradlew :app:assembleDebug

test:
	./gradlew test

install: debug
	adb install -r app/build/outputs/apk/debug/app-debug.apk

release:
	./gradlew :app:assembleRelease

clean:
	./gradlew clean
