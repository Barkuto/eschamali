#!/bin/bash
if [[ ! -d "/data/.git" ]]; then
	git clone https://github.com/Barkuto/eschamali.git /data
else
	cd /data && git checkout -- . && git pull
fi