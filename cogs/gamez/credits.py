import importlib
import os
from datetime import datetime

UTILS = importlib.import_module('.utils', 'util')
DB_MOD = UTILS.DB_MOD
DB = DB_MOD.DB
LOGGER = UTILS.VARS.LOGGER

GAMES_TABLE = 'games'
GAMES_TABLE_COL1 = ('discord_id', DB_MOD.INTEGER)
GAMES_TABLE_COL2 = ('credits', DB_MOD.INTEGER)
DAILIES_TABLE = 'daily'
DAILIES_TABLE_COL1 = ('discord_id', DB_MOD.INTEGER)
DAILIES_TABLE_COL2 = ('timestamp', DB_MOD.INTEGER)
DB_PATH = os.path.join(os.path.dirname(__file__),  'games.db')

HOUSE_DEFAULT = 1000000
USER_DEFAULT = 1000


class Credits():
    def __init__(self, house_user, house_default=HOUSE_DEFAULT, user_default=USER_DEFAULT):
        self.house_default = house_default
        self.user_default = user_default

        db = DB(DB_PATH)
        if not db.create_table(GAMES_TABLE, GAMES_TABLE_COL1, GAMES_TABLE_COL2):
            LOGGER.error('Could not create games table.')
        if not db.create_table(DAILIES_TABLE, DAILIES_TABLE_COL1, DAILIES_TABLE_COL2):
            LOGGER.error('Could not create dailies table.')
        self.get_user_creds(house_user, self.house_default)

    def _get_db(self):
        return DB(DB_PATH)

    def get_all_user_creds(self):
        return self._get_db().get_all(GAMES_TABLE)

    def get_user_creds(self, user, default=USER_DEFAULT):
        db = self._get_db()
        creds = db.get_value(GAMES_TABLE, GAMES_TABLE_COL2[0], (GAMES_TABLE_COL1[0], user.id))
        if creds is None:
            if not db.insert_row(GAMES_TABLE, (GAMES_TABLE_COL1[0], user.id), (GAMES_TABLE_COL2[0], default)):
                LOGGER.error(f'Could not create games entry for {user}({user.id})')
            return default
        return creds

    def add_user_creds(self, user, amount):
        db = self._get_db()
        creds = db.get_value(GAMES_TABLE, GAMES_TABLE_COL2[0], (GAMES_TABLE_COL1[0], user.id))
        if not creds:
            creds = self.get_user_creds(user)
        creds += amount
        if not db.update_row(GAMES_TABLE, (GAMES_TABLE_COL1[0], user.id), (GAMES_TABLE_COL2[0], creds)):
            LOGGER.error(f'Could not update games entry for {user}({user.id})')

    def transfer_from_to(self, user1, user2, amount):
        self.add_user_creds(user1, -amount)
        self.add_user_creds(user2, amount)

    def daily(self, user):
        db = self._get_db()
        last_daily = db.get_value(DAILIES_TABLE, DAILIES_TABLE_COL2[0], (DAILIES_TABLE_COL1[0], user.id))
        now = datetime.now().timestamp()
        give = False
        if not last_daily:
            give = True
            last_daily = now
            db.insert_row(DAILIES_TABLE, (DAILIES_TABLE_COL1[0], user.id), (DAILIES_TABLE_COL2[0], now))

        if give or (last_daily + (24 * 60 * 60)) - now <= 0:
            self.add_user_creds(user, 1000)
            return True
        diff = str(datetime.fromtimestamp(last_daily + (24 * 60 * 60)) - datetime.fromtimestamp(now))
        diff = diff.split('.')[0]
        return diff
