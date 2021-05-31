import importlib
from cogs.gamez import blackjack as bj


class Stats():
    def __init__(self, bot, db_path, valid_games):
        importlib.reload(bj)

        self.BJ_STATS_TABLE = 'blackjack_stats'
        self.BJ_STATS_TABLE_COL1 = ('field', bot.db.TEXT)
        self.BJ_STATS_TABLE_COL2 = ('bot_wins', bot.db.INTEGER)
        self.BJ_STATS_TABLE_COL3 = ('bot_losses', bot.db.INTEGER)
        self.BJ_STATS_TABLE_COL4 = ('user_wins', bot.db.INTEGER)
        self.BJ_STATS_TABLE_COL5 = ('user_losses', bot.db.INTEGER)
        self.BJ_STATS_TABLE_COL6 = ('draws', bot.db.INTEGER)

        self.BJ_DOUBLED_FIELD = 'doubled'
        self.BJ_SPLITS_FIELD = 'splits'
        self.BJ_BUSTS_FIELD = 'busts'

        self.BJ_PERSONAL_STATS_TABLE = 'blackjack_personal_stats'
        self.BJ_PERSONAL_STATS_TABLE_COL1 = ('discord_id', bot.db.INTEGER)
        self.BJ_PERSONAL_STATS_TABLE_COL2 = ('wins', bot.db.INTEGER)
        self.BJ_PERSONAL_STATS_TABLE_COL3 = ('losses', bot.db.INTEGER)
        self.BJ_PERSONAL_STATS_TABLE_COL4 = ('draws', bot.db.INTEGER)

        self.bot = bot
        self.valid_games = valid_games
        self.db_path = db_path
        self._init_stats_db()

    def _get_stats_db(self):
        return self.bot.db.DB(self.db_path)

    def is_valid_game(self, game):
        return game in self.valid_games

    def _init_stats_db(self):
        db = self._get_stats_db()
        #############
        # Blackjack #
        #############
        # Global Stats
        if not db.create_table(self.BJ_STATS_TABLE,
                               self.BJ_STATS_TABLE_COL1, self.BJ_STATS_TABLE_COL2, self.BJ_STATS_TABLE_COL3,
                               self.BJ_STATS_TABLE_COL4, self.BJ_STATS_TABLE_COL5, self.BJ_STATS_TABLE_COL6):
            self.bot.vars.LOGGER.error('Could not create stats table.')
        if not db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], self.BJ_DOUBLED_FIELD)):
            if not db.insert_row(self.BJ_STATS_TABLE,
                                 (self.BJ_STATS_TABLE_COL1[0], self.BJ_DOUBLED_FIELD),
                                 (self.BJ_STATS_TABLE_COL2[0], 0), (self.BJ_STATS_TABLE_COL3[0], 0),
                                 (self.BJ_STATS_TABLE_COL4[0], 0), (self.BJ_STATS_TABLE_COL5[0], 0),
                                 (self.BJ_STATS_TABLE_COL6[0], 0)):
                self.bot.vars.LOGGER.error(f'Could not create blackjack stats row: "{self.BJ_DOUBLED_FIELD}"')
        if not db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], self.BJ_SPLITS_FIELD)):
            if not db.insert_row(self.BJ_STATS_TABLE,
                                 (self.BJ_STATS_TABLE_COL1[0], self.BJ_SPLITS_FIELD), (self.BJ_STATS_TABLE_COL2[0], 0),
                                 (self.BJ_STATS_TABLE_COL3[0], 0), (self.BJ_STATS_TABLE_COL4[0], 0),
                                 (self.BJ_STATS_TABLE_COL5[0], 0), (self.BJ_STATS_TABLE_COL6[0], 0)):
                self.bot.vars.LOGGER.error(f'Could not create blackjack stats row: "{self.BJ_SPLITS_FIELD}"')
        if not db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], self.BJ_BUSTS_FIELD)):
            if not db.insert_row(self.BJ_STATS_TABLE,
                                 (self.BJ_STATS_TABLE_COL1[0], self.BJ_BUSTS_FIELD), (self.BJ_STATS_TABLE_COL2[0], 0),
                                 (self.BJ_STATS_TABLE_COL3[0], 0), (self.BJ_STATS_TABLE_COL4[0], 0),
                                 (self.BJ_STATS_TABLE_COL5[0], 0), (self.BJ_STATS_TABLE_COL6[0], 0)):
                self.bot.vars.LOGGER.error(f'Could not create blackjack stats row: "{self.BJ_BUSTS_FIELD}"')
        for i in range(2, 22):
            val = str(i)
            if not db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], val)):
                if not db.insert_row(self.BJ_STATS_TABLE,
                                     (self.BJ_STATS_TABLE_COL1[0], val), (self.BJ_STATS_TABLE_COL2[0], 0),
                                     (self.BJ_STATS_TABLE_COL3[0], 0), (self.BJ_STATS_TABLE_COL4[0], 0),
                                     (self.BJ_STATS_TABLE_COL5[0], 0), (self.BJ_STATS_TABLE_COL6[0], 0)):
                    self.bot.vars.LOGGER.error(f'Could not create blackjack stats row: "{val}"')
        # Personal Stats
        if not db.create_table(self.BJ_PERSONAL_STATS_TABLE,
                               self.BJ_PERSONAL_STATS_TABLE_COL1, self.BJ_PERSONAL_STATS_TABLE_COL2,
                               self.BJ_PERSONAL_STATS_TABLE_COL3, self.BJ_PERSONAL_STATS_TABLE_COL4):
            self.bot.vars.LOGGER.error('Could not create personal stats table.')

    async def get_bj_global_stats(self):
        db = self._get_stats_db()
        rows = db.get_all(self.BJ_STATS_TABLE)
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
            rows = db.get_all(self.BJ_STATS_TABLE)
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
            row = db.get_row(self.BJ_PERSONAL_STATS_TABLE, (self.BJ_PERSONAL_STATS_TABLE_COL1[0], user.id))

            wins = row[1] if row else 0
            losses = row[2] if row else 0
            draws = row[3] if row else 0
        return (wins, losses, draws)

    async def save_bj_stats(self, bj_state, user_id):
        if bj_state:
            # save stats
            doubled = bj_state.doubled
            house_sum = str(bj.best_sum(bj_state.house_cards))
            player_sums = [bj.best_sum(cards) for cards in bj_state.player_cards]
            states = bj_state.states
            split = len(states) > 1
            db = self._get_stats_db()
            for i in range(len(states)):
                s = states[i]
                p_sum = str(player_sums[i])
                doubled_row = db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], self.BJ_DOUBLED_FIELD), factory=True)
                splits_row = db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], self.BJ_SPLITS_FIELD), factory=True)
                busts_row = db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], self.BJ_BUSTS_FIELD), factory=True)
                p_sum_row = db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], p_sum), factory=True)
                h_sum_row = db.get_row(self.BJ_STATS_TABLE, (self.BJ_STATS_TABLE_COL1[0], house_sum), factory=True)
                # personal stat records
                player_wins = 0
                player_losses = 0
                player_draws = 0
                # HAND WIN/LOSS/DRAWS
                if s == bj.PLAYER_WIN:
                    player_wins += 1
                    # +1 p_sum[user_wins]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], p_sum),
                                  (self.BJ_STATS_TABLE_COL4[0], p_sum_row['user_wins'] + 1))
                    # +1 h_sum[bot_losses]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], house_sum),
                                  (self.BJ_STATS_TABLE_COL3[0], h_sum_row['bot_losses'] + 1))
                elif s == bj.PLAYER_LOSE:
                    player_losses += 1
                    # +1 p_sum[user_losses]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], p_sum),
                                  (self.BJ_STATS_TABLE_COL5[0], p_sum_row['user_losses'] + 1))
                    # +1 h_sum[bot_wins]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], house_sum),
                                  (self.BJ_STATS_TABLE_COL2[0], h_sum_row['bot_wins'] + 1))
                elif s == bj.PLAYER_BUST:
                    player_losses += 1
                    # +1 busts[user_losses]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], self.BJ_BUSTS_FIELD),
                                  (self.BJ_STATS_TABLE_COL5[0], busts_row['user_losses'] + 1))
                    # +1 busts[bot_wins]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], self.BJ_BUSTS_FIELD),
                                  (self.BJ_STATS_TABLE_COL2[0], busts_row['bot_wins'] + 1))
                elif s == bj.HOUSE_BUST:
                    player_wins += 1
                    # +1 busts[user_wins]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], self.BJ_BUSTS_FIELD),
                                  (self.BJ_STATS_TABLE_COL4[0], busts_row['user_wins'] + 1))
                    # +1 busts[bot_losses]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], self.BJ_BUSTS_FIELD),
                                  (self.BJ_STATS_TABLE_COL3[0], busts_row['bot_losses'] + 1))
                elif s == bj.DRAW:
                    player_draws += 1
                    # +1 p_sum/h_sum[draws]
                    db.update_row(self.BJ_STATS_TABLE,
                                  (self.BJ_STATS_TABLE_COL1[0], p_sum),
                                  (self.BJ_STATS_TABLE_COL6[0], p_sum_row['draws'] + 1))
                # DOUBLED WIN/LOSS/DRAWS
                if doubled[i]:
                    if s in [bj.PLAYER_WIN, bj.HOUSE_BUST]:
                        # +1 doubled[user_wins]
                        db.update_row(self.BJ_STATS_TABLE,
                                      (self.BJ_STATS_TABLE_COL1[0], self.BJ_DOUBLED_FIELD),
                                      (self.BJ_STATS_TABLE_COL4[0], doubled_row['user_wins'] + 1))
                    elif s in [bj.PLAYER_LOSE, bj.PLAYER_BUST]:
                        # +1 doubled[user_losses]
                        db.update_row(self.BJ_STATS_TABLE,
                                      (self.BJ_STATS_TABLE_COL1[0], self.BJ_DOUBLED_FIELD),
                                      (self.BJ_STATS_TABLE_COL5[0], doubled_row['user_losses'] + 1))
                    elif s == bj.DRAW:
                        # +1 doubled[draws]
                        db.update_row(self.BJ_STATS_TABLE,
                                      (self.BJ_STATS_TABLE_COL1[0], self.BJ_DOUBLED_FIELD),
                                      (self.BJ_STATS_TABLE_COL6[0], doubled_row['draws'] + 1))
                if split:
                    if s in [bj.PLAYER_WIN, bj.HOUSE_BUST]:
                        # +1 splits[user_wins]
                        db.update_row(self.BJ_STATS_TABLE,
                                      (self.BJ_STATS_TABLE_COL1[0], self.BJ_SPLITS_FIELD),
                                      (self.BJ_STATS_TABLE_COL4[0], splits_row['user_wins'] + 1))
                    elif s in [bj.PLAYER_LOSE, bj.PLAYER_BUST]:
                        # +1 splits[user_losses]
                        db.update_row(self.BJ_STATS_TABLE,
                                      (self.BJ_STATS_TABLE_COL1[0], self.BJ_SPLITS_FIELD),
                                      (self.BJ_STATS_TABLE_COL5[0], splits_row['user_losses'] + 1))
                    elif s == bj.DRAW:
                        # +1 splits[draws]
                        db.update_row(self.BJ_STATS_TABLE,
                                      (self.BJ_STATS_TABLE_COL1[0], self.BJ_SPLITS_FIELD),
                                      (self.BJ_STATS_TABLE_COL6[0], splits_row['draws'] + 1))
                # save personal stats
                user_row = db.get_row(self.BJ_PERSONAL_STATS_TABLE, (self.BJ_PERSONAL_STATS_TABLE_COL1[0], user_id))
                if not user_row:
                    db.insert_row(self.BJ_PERSONAL_STATS_TABLE,
                                  (self.BJ_PERSONAL_STATS_TABLE_COL1[0], user_id),
                                  (self.BJ_PERSONAL_STATS_TABLE_COL2[0], player_wins),
                                  (self.BJ_PERSONAL_STATS_TABLE_COL3[0], player_losses),
                                  (self.BJ_PERSONAL_STATS_TABLE_COL4[0], player_draws))
                else:
                    db.update_row(self.BJ_PERSONAL_STATS_TABLE,
                                  (self.BJ_PERSONAL_STATS_TABLE_COL1[0], user_id),
                                  (self.BJ_PERSONAL_STATS_TABLE_COL2[0], user_row[1] + player_wins),
                                  (self.BJ_PERSONAL_STATS_TABLE_COL3[0], user_row[2] + player_losses),
                                  (self.BJ_PERSONAL_STATS_TABLE_COL4[0], user_row[3] + player_draws))
