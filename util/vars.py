import os
import os.path

import logging
from logging.handlers import TimedRotatingFileHandler

LOGGER = logging.getLogger('discord')
COGS_DIR_NAME = 'cogs'
COGS_DIR = os.path.join(os.path.dirname(__file__), '..', COGS_DIR_NAME, '')
BASE_COGS_DIR_NAME = 'base'
LOGS_DIR = os.path.join(os.path.dirname(__file__), '..', 'logs', '')
LOGS_INFO_DIR = os.path.join(LOGS_DIR, 'info', '')
LOGS_ERROR_DIR = os.path.join(LOGS_DIR, 'error', '')
DB_DIR = os.path.join(os.path.dirname(__file__), '..', 'servers', '')
CONFIG_FILE = os.path.join(os.path.dirname(__file__), '..', 'config.json')


def setup_logger():
    os.makedirs(LOGS_INFO_DIR, exist_ok=True)
    os.makedirs(LOGS_ERROR_DIR, exist_ok=True)

    FORMAT = logging.Formatter('%(asctime)s %(levelname)s: %(message)s', '%Y-%m-%d %H:%M:%S')

    LOGGER.setLevel(logging.DEBUG)

    debugHandler = logging.StreamHandler()
    debugHandler.setLevel(logging.DEBUG)
    debugHandler.setFormatter(FORMAT)
    debugHandler.addFilter(lambda record: not ('Shard ID' in record.msg
                                               or 'socket_raw_send' in record.args
                                               or 'socket_raw_receive' in record.args
                                               or 'socket_response' in record.args))

    errorHandler = TimedRotatingFileHandler(LOGS_ERROR_DIR + 'error.log', when='midnight')
    errorHandler.setLevel(logging.ERROR)
    errorHandler.setFormatter(FORMAT)

    infoHandler = TimedRotatingFileHandler(LOGS_INFO_DIR + 'info.log', when='midnight')
    infoHandler.setLevel(logging.INFO)
    infoHandler.setFormatter(FORMAT)
    infoHandler.addFilter(lambda record: record.name == 'discord'
                          and record.levelno == logging.INFO)

    LOGGER.addHandler(debugHandler)
    LOGGER.addHandler(errorHandler)
    LOGGER.addHandler(infoHandler)


setup_logger()
