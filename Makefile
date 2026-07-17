IMAGE = kotlin-dev
CONTAINER = kotlin-dev-container
APK = app/build/outputs/apk/debug/app-debug.apk

image:
	docker build -t $(IMAGE) .

container: image
	@[ "$$(docker container inspect -f '{{.State.Running}}' $(CONTAINER) 2>/dev/null)" = "true" ] || \
	docker run -d --rm --name $(CONTAINER) -u $$(id -u):$$(id -g) -v .:/src -v ./.gradle-docker:/gradle $(IMAGE) sleep infinity

shell: container 
	docker exec -u $$(id -u):$$(id -g) -it -w /src $(CONTAINER) bash

build: container 
	docker exec -u $$(id -u):$$(id -g) -w /src $(CONTAINER) ./gradlew assembleDebug

install: build 
	adb install -r $(APK)

run: install
	adb shell am start -n $(APPID)/.MainActivity


.PHONY: build install shell image container
