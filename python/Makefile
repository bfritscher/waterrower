all: virtualenv

test:
	@echo running tests
.PHONY: test

deps:
	@echo updating dependencies
	var/bin/pip install -r requirements.txt
.PHONY deps

virtualenv: 
	virtualenv --no-site-packages --python=python2.7 --clear rower-env
.PHONY: virtualenv

