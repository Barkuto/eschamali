FROM python:3.8

COPY docker/*.sh /root/
RUN chmod 0755 /root/*.sh

WORKDIR /data

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

VOLUME /data

CMD /bin/bash /root/install.sh && /bin/bash /root/start.sh