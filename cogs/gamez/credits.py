import os
from datetime import datetime

HOUSE_DEFAULT = 1000000
USER_DEFAULT = 1000


class Credits():
    def __init__(self, bot, house_default=HOUSE_DEFAULT, user_default=USER_DEFAULT):
        self.GAMES_TABLE = 'games'
        self.GAMES_TABLE_COL1 = ('discord_id', bot.db.INTEGER)
        self.GAMES_TABLE_COL2 = ('credits', bot.db.INTEGER)

        self.DAILIES_TABLE = 'daily'
        self.DAILIES_TABLE_COL1 = ('discord_id', bot.db.INTEGER)
        self.DAILIES_TABLE_COL2 = ('timestamp', bot.db.INTEGER)
        self.DB_PATH = os.path.join(os.path.dirname(__file__),  'games.db')

        self.bot = bot
        self.house_default = house_default
        self.user_default = user_default

        db = self.bot.db.DB(self.DB_PATH)
        if not db.create_table(self.GAMES_TABLE, self.GAMES_TABLE_COL1, self.GAMES_TABLE_COL2):
            self.bot.vars.LOGGER.error('Could not create games table.')
        if not db.create_table(self.DAILIES_TABLE, self.DAILIES_TABLE_COL1, self.DAILIES_TABLE_COL2):
            self.bot.vars.LOGGER.error('Could not create dailies table.')
        self.get_user_creds(bot.user, self.house_default)

    def _get_db(self):
        return self.bot.db.DB(self.DB_PATH)

    def get_all_user_creds(self):
        return self._get_db().get_all(self.GAMES_TABLE)

    def get_user_creds(self, user, default=USER_DEFAULT):
        db = self._get_db()
        creds = db.get_value(self.GAMES_TABLE, self.GAMES_TABLE_COL2[0], (self.GAMES_TABLE_COL1[0], user.id))
        if creds is None:
            if not db.insert_row(self.GAMES_TABLE, (self.GAMES_TABLE_COL1[0], user.id), (self.GAMES_TABLE_COL2[0], default)):
                self.bot.vars.LOGGER.error(f'Could not create games entry for {user}({user.id})')
            return default
        return creds

    def add_user_creds(self, user, amount):
        db = self._get_db()
        creds = db.get_value(self.GAMES_TABLE, self.GAMES_TABLE_COL2[0], (self.GAMES_TABLE_COL1[0], user.id))
        if not creds:
            creds = self.get_user_creds(user)
        creds += amount
        if not db.update_row(self.GAMES_TABLE, (self.GAMES_TABLE_COL1[0], user.id), (self.GAMES_TABLE_COL2[0], creds)):
            self.bot.vars.LOGGER.error(f'Could not update games entry for {user}({user.id})')

    def transfer_from_to(self, user1, user2, amount):
        self.add_user_creds(user1, -amount)
        self.add_user_creds(user2, amount)

    def daily(self, user):
        db = self._get_db()
        last_daily = db.get_value(self.DAILIES_TABLE, self.DAILIES_TABLE_COL2[0], (self.DAILIES_TABLE_COL1[0], user.id))
        now = datetime.now().timestamp()
        give = False
        if not last_daily:
            give = True
            last_daily = now
            db.insert_row(self.DAILIES_TABLE, (self.DAILIES_TABLE_COL1[0], user.id), (self.DAILIES_TABLE_COL2[0], now))

        daily_time = 20 * 60 * 60
        if give or (last_daily + daily_time) - now <= 0:
            self.add_user_creds(user, 1000)
            db.update_row(self.DAILIES_TABLE, (self.DAILIES_TABLE_COL1[0], user.id), (self.DAILIES_TABLE_COL2[0], now))
            return True
        diff = str(datetime.fromtimestamp(last_daily + daily_time) - datetime.fromtimestamp(now))
        diff = diff.split('.')[0]
        return diff
