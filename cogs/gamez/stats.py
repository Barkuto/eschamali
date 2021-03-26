import importlib
import os

UTILS = importlib.import_module('.utils', 'util')
BJ_MOD = importlib.import_module('.blackjack', 'cogs.gamez')

importlib.reload(UTILS)
importlib.reload(BJ_MOD)

DB_MOD = UTILS.DB_MOD
DB = DB_MOD.DB
LOGGER = UTILS.VARS.LOGGER

BJ_STATS_TABLE = 'blackjack_stats'
BJ_STATS_TABLE_COL1 = ('field', DB_MOD.TEXT)
BJ_STATS_TABLE_COL2 = ('bot_wins', DB_MOD.INTEGER)
BJ_STATS_TABLE_COL3 = ('bot_losses', DB_MOD.INTEGER)
BJ_STATS_TABLE_COL4 = ('user_wins', DB_MOD.INTEGER)
BJ_STATS_TABLE_COL5 = ('user_losses', DB_MOD.INTEGER)
BJ_STATS_TABLE_COL6 = ('draws', DB_MOD.INTEGER)
BJ_DOUBLED_FIELD = 'doubled'
BJ_SPLITS_FIELD = 'splits'
BJ_BUSTS_FIELD = 'busts'

BJ_PERSONAL_STATS_TABLE = 'blackjack_personal_stats'
BJ_PERSONAL_STATS_TABLE_COL1 = ('discord_id', DB_MOD.INTEGER)
BJ_PERSONAL_STATS_TABLE_COL2 = ('wins', DB_MOD.INTEGER)
BJ_PERSONAL_STATS_TABLE_COL3 = ('losses', DB_MOD.INTEGER)
BJ_PERSONAL_STATS_TABLE_COL4 = ('draws', DB_MOD.INTEGER)

BLACKJACK = ['blackjack', 'bj']
VALID_GAMES = [] + BLACKJACK


class Stats():
    def __init__(self, bot, db_path):
        self.bot = bot
        self.db_path = db_path
        self._init_stats_db()

    def _get_stats_db(self):
        return DB(self.db_path)

    def is_valid_game(self, game):
        return game in VALID_GAMES

    def _init_stats_db(self):
        db = self._get_stats_db()
        #############
        # Blackjack #
        #############
        # Global Stats
        if not db.create_table(BJ_STATS_TABLE,
                               BJ_STATS_TABLE_COL1, BJ_STATS_TABLE_COL2, BJ_STATS_TABLE_COL3,
                               BJ_STATS_TABLE_COL4, BJ_STATS_TABLE_COL5, BJ_STATS_TABLE_COL6):
            LOGGER.error('Could not create stats table.')
        if not db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], BJ_DOUBLED_FIELD)):
            if not db.insert_row(BJ_STATS_TABLE,
                                 (BJ_STATS_TABLE_COL1[0], BJ_DOUBLED_FIELD),
                                 (BJ_STATS_TABLE_COL2[0], 0), (BJ_STATS_TABLE_COL3[0], 0),
                                 (BJ_STATS_TABLE_COL4[0], 0), (BJ_STATS_TABLE_COL5[0], 0),
                                 (BJ_STATS_TABLE_COL6[0], 0)):
                LOGGER.error(f'Could not create blackjack stats row: "{BJ_DOUBLED_FIELD}"')
        if not db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], BJ_SPLITS_FIELD)):
            if not db.insert_row(BJ_STATS_TABLE,
                                 (BJ_STATS_TABLE_COL1[0], BJ_SPLITS_FIELD), (BJ_STATS_TABLE_COL2[0], 0),
                                 (BJ_STATS_TABLE_COL3[0], 0), (BJ_STATS_TABLE_COL4[0], 0),
                                 (BJ_STATS_TABLE_COL5[0], 0), (BJ_STATS_TABLE_COL6[0], 0)):
                LOGGER.error(f'Could not create blackjack stats row: "{BJ_SPLITS_FIELD}"')
        if not db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], BJ_BUSTS_FIELD)):
            if not db.insert_row(BJ_STATS_TABLE,
                                 (BJ_STATS_TABLE_COL1[0], BJ_BUSTS_FIELD), (BJ_STATS_TABLE_COL2[0], 0),
                                 (BJ_STATS_TABLE_COL3[0], 0), (BJ_STATS_TABLE_COL4[0], 0),
                                 (BJ_STATS_TABLE_COL5[0], 0), (BJ_STATS_TABLE_COL6[0], 0)):
                LOGGER.error(f'Could not create blackjack stats row: "{BJ_BUSTS_FIELD}"')
        for i in range(2, 22):
            val = str(i)
            if not db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], val)):
                if not db.insert_row(BJ_STATS_TABLE,
                                     (BJ_STATS_TABLE_COL1[0], val), (BJ_STATS_TABLE_COL2[0], 0),
                                     (BJ_STATS_TABLE_COL3[0], 0), (BJ_STATS_TABLE_COL4[0], 0),
                                     (BJ_STATS_TABLE_COL5[0], 0), (BJ_STATS_TABLE_COL6[0], 0)):
                    LOGGER.error(f'Could not create blackjack stats row: "{val}"')
        # Personal Stats
        if not db.create_table(BJ_PERSONAL_STATS_TABLE,
                               BJ_PERSONAL_STATS_TABLE_COL1, BJ_PERSONAL_STATS_TABLE_COL2,
                               BJ_PERSONAL_STATS_TABLE_COL3, BJ_PERSONAL_STATS_TABLE_COL4):
            LOGGER.error('Could not create personal stats table.')

    async def get_bj_global_stats(self):
        db = self._get_stats_db()
        rows = db.get_all(BJ_STATS_TABLE)
        doubled = None
        splits = []
        busts = []
        nums = [[]] * 22
        for r in rows:
            if r[0] == 'doubled':
                doubled = r
            elif r[0] == 'splits':
                splits = r
            elif r[0] == 'busts':
                busts = r
            else:
                nums[int(r[0])] = r
        nums = nums[2:]
        return (doubled, splits, busts, nums)

    async def get_bj_personal_stats(self, user):
        db = self._get_stats_db()
        if user.id == self.bot.user.id:
            rows = db.get_all(BJ_STATS_TABLE)
            busts = []
            nums = [[]] * 22
            for r in rows:
                if r[0] == 'doubled':
                    pass
                elif r[0] == 'splits':
                    pass
                elif r[0] == 'busts':
                    busts = r
                else:
                    nums[int(r[0])] = r
            nums = nums[2:]

            wins = busts[4] + sum([r[1] for r in nums])
            losses = busts[2] + sum([r[2] for r in nums])
            draws = sum([r[5] for r in nums])
        else:
            row = db.get_row(BJ_PERSONAL_STATS_TABLE, (BJ_PERSONAL_STATS_TABLE_COL1[0], user.id))

            wins = row[1] if row else 0
            losses = row[2] if row else 0
            draws = row[3] if row else 0
        return (wins, losses, draws)

    async def save_bj_stats(self, bj_state, user_id):
        if bj_state:
            # save stats
            doubled = bj_state.doubled
            house_sum = str(BJ_MOD.best_sum(bj_state.house_cards))
            player_sums = [BJ_MOD.best_sum(cards) for cards in bj_state.player_cards]
            states = bj_state.states
            split = len(states) > 1
            db = self._get_stats_db()
            for i in range(len(states)):
                s = states[i]
                p_sum = str(player_sums[i])
                doubled_row = db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], BJ_DOUBLED_FIELD), factory=True)
                splits_row = db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], BJ_SPLITS_FIELD), factory=True)
                busts_row = db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], BJ_BUSTS_FIELD), factory=True)
                p_sum_row = db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], p_sum), factory=True)
                h_sum_row = db.get_row(BJ_STATS_TABLE, (BJ_STATS_TABLE_COL1[0], house_sum), factory=True)
                # personal stat records
                player_wins = 0
                player_losses = 0
                player_draws = 0
                # HAND WIN/LOSS/DRAWS
                if s == BJ_MOD.PLAYER_WIN:
                    player_wins += 1
                    # +1 p_sum[user_wins]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], p_sum),
                                  (BJ_STATS_TABLE_COL4[0], p_sum_row['user_wins'] + 1))
                    # +1 h_sum[bot_losses]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], house_sum),
                                  (BJ_STATS_TABLE_COL3[0], h_sum_row['bot_losses'] + 1))
                elif s == BJ_MOD.PLAYER_LOSE:
                    player_losses += 1
                    # +1 p_sum[user_losses]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], p_sum),
                                  (BJ_STATS_TABLE_COL5[0], p_sum_row['user_losses'] + 1))
                    # +1 h_sum[bot_wins]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], house_sum),
                                  (BJ_STATS_TABLE_COL2[0], h_sum_row['bot_wins'] + 1))
                elif s == BJ_MOD.PLAYER_BUST:
                    player_losses += 1
                    # +1 busts[user_losses]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], BJ_BUSTS_FIELD),
                                  (BJ_STATS_TABLE_COL5[0], busts_row['user_losses'] + 1))
                    # +1 busts[bot_wins]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], BJ_BUSTS_FIELD),
                                  (BJ_STATS_TABLE_COL2[0], busts_row['bot_wins'] + 1))
                elif s == BJ_MOD.HOUSE_BUST:
                    player_wins += 1
                    # +1 busts[user_wins]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], BJ_BUSTS_FIELD),
                                  (BJ_STATS_TABLE_COL4[0], busts_row['user_wins'] + 1))
                    # +1 busts[bot_losses]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], BJ_BUSTS_FIELD),
                                  (BJ_STATS_TABLE_COL3[0], busts_row['bot_losses'] + 1))
                elif s == BJ_MOD.DRAW:
                    player_draws += 1
                    # +1 p_sum/h_sum[draws]
                    db.update_row(BJ_STATS_TABLE,
                                  (BJ_STATS_TABLE_COL1[0], p_sum),
                                  (BJ_STATS_TABLE_COL6[0], p_sum_row['draws'] + 1))
                # DOUBLED WIN/LOSS/DRAWS
                if doubled[i]:
                    if s in [BJ_MOD.PLAYER_WIN, BJ_MOD.HOUSE_BUST]:
                        # +1 doubled[user_wins]
                        db.update_row(BJ_STATS_TABLE,
                                      (BJ_STATS_TABLE_COL1[0], BJ_DOUBLED_FIELD),
                                      (BJ_STATS_TABLE_COL4[0], doubled_row['user_wins'] + 1))
                    elif s in [BJ_MOD.PLAYER_LOSE, BJ_MOD.PLAYER_BUST]:
                        # +1 doubled[user_losses]
                        db.update_row(BJ_STATS_TABLE,
                                      (BJ_STATS_TABLE_COL1[0], BJ_DOUBLED_FIELD),
                                      (BJ_STATS_TABLE_COL5[0], doubled_row['user_losses'] + 1))
                    elif s == BJ_MOD.DRAW:
                        # +1 doubled[draws]
                        db.update_row(BJ_STATS_TABLE,
                                      (BJ_STATS_TABLE_COL1[0], BJ_DOUBLED_FIELD),
                                      (BJ_STATS_TABLE_COL6[0], doubled_row['draws'] + 1))
                if split:
                    if s in [BJ_MOD.PLAYER_WIN, BJ_MOD.HOUSE_BUST]:
                        # +1 splits[user_wins]
                        db.update_row(BJ_STATS_TABLE,
                                      (BJ_STATS_TABLE_COL1[0], BJ_SPLITS_FIELD),
                                      (BJ_STATS_TABLE_COL4[0], splits_row['user_wins'] + 1))
                    elif s in [BJ_MOD.PLAYER_LOSE, BJ_MOD.PLAYER_BUST]:
                        # +1 splits[user_losses]
                        db.update_row(BJ_STATS_TABLE,
                                      (BJ_STATS_TABLE_COL1[0], BJ_SPLITS_FIELD),
                                      (BJ_STATS_TABLE_COL5[0], splits_row['user_losses'] + 1))
                    elif s == BJ_MOD.DRAW:
                        # +1 splits[draws]
                        db.update_row(BJ_STATS_TABLE,
                                      (BJ_STATS_TABLE_COL1[0], BJ_SPLITS_FIELD),
                                      (BJ_STATS_TABLE_COL6[0], splits_row['draws'] + 1))
                # save personal stats
                user_row = db.get_row(BJ_PERSONAL_STATS_TABLE, (BJ_PERSONAL_STATS_TABLE_COL1[0], user_id))
                if not user_row:
                    db.insert_row(BJ_PERSONAL_STATS_TABLE,
                                  (BJ_PERSONAL_STATS_TABLE_COL1[0], user_id),
                                  (BJ_PERSONAL_STATS_TABLE_COL2[0], player_wins),
                                  (BJ_PERSONAL_STATS_TABLE_COL3[0], player_losses),
                                  (BJ_PERSONAL_STATS_TABLE_COL4[0], player_draws))
                else:
                    db.update_row(BJ_PERSONAL_STATS_TABLE,
                                  (BJ_PERSONAL_STATS_TABLE_COL1[0], user_id),
                                  (BJ_PERSONAL_STATS_TABLE_COL2[0], user_row[1] + player_wins),
                                  (BJ_PERSONAL_STATS_TABLE_COL3[0], user_row[2] + player_losses),
                                  (BJ_PERSONAL_STATS_TABLE_COL4[0], user_row[3] + player_draws))
