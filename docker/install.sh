#!/bin/bash
if [[ ! -d "/data/.git" ]]; then
	git clone https://github.com/Barkuto/Eschamali.git /data
else
	cd /data && git checkout -- . && git pull
fi