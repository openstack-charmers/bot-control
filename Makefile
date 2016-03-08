#!/usr/bin/make
PYTHON := /usr/bin/env python

lint:
	@flake8 tools/lib
