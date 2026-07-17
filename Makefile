IMAGE     = kotlin-dev
CONTAINER = kotlin-dev-container
APK       = app/build/outputs/apk/debug/app-debug.apk
APPID     = com.eitan.trainer
UID_GID   = $(shell id -u):$(shell id -g)
DEXEC     = docker exec -u $(UID_GID) -e HOME=/gradle -w /src $(CONTAINER)

image:
	docker build --provenance=false -t $(IMAGE) .

container: image
	@mkdir -p .gradle-docker
	@if [ "$$(docker inspect -f '{{.Image}}' $(CONTAINER) 2>/dev/null)" != \
		"$$(docker image inspect -f '{{.Id}}' $(IMAGE))" ]; then \
	    docker rm -f $(CONTAINER) >/dev/null 2>&1 || true; \
	    docker run -d --rm --name $(CONTAINER) -u $(UID_GID) -e HOME=/gradle \
	        -v $(CURDIR):/src -v $(CURDIR)/.gradle-docker:/gradle $(IMAGE) sleep infinity; \
	fi

build: container
	$(DEXEC) ./gradlew assembleDebug

shell: container
	docker exec -it -u $(UID_GID) -e HOME=/gradle -w /src $(CONTAINER) bash

install: build
	adb install -r $(APK)

run: install
	adb shell am start -n $(APPID)/.MainActivity

.PHONY: image container build shell install run

