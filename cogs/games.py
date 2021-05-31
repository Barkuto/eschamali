import importlib
import os
import random
import re
import base64
import json
import requests
from discord import Embed, User, Colour, Member
from discord.ext import commands
from io import BytesIO
from cogs.gamez import blackjack as bj
from cogs.gamez import card_imgs, credits, deck, stats

BJ_DEFAULT_BET = 100
VALID_GAMES_HELP = 'Valid Games so far: "blackjack"'


class Games(commands.Cog):
    """Fun commands"""

    def __init__(self, bot):
        importlib.reload(bj)
        importlib.reload(card_imgs)
        card_imgs.reload()
        importlib.reload(credits)
        importlib.reload(deck)
        importlib.reload(stats)

        self.ANSWERS = [
            'It is certain',
            'It is decidedly so',
            'Without a doubt',
            'Yes, definitely',
            'You may rely on it',
            'As I see it, yes',
            'Most likely',
            'Outlook good',
            'Yes',
            'Signs point to yes',

            'Reply hazy try again',
            'Ask again later',
            'Better not tell you now',
            'Cannot predict now',
            'Concentrate and ask again',

            'Don\'t count on it',
            'My reply is no',
            'My sources say no',
            'Outlook not so good',
            'Very doubtful',
            'NO - May cause disease contraction'
        ]

        self.PICKS = {
            0: ':rock:',
            1: ':newspaper:',
            2: ':scissors:'
        }

        self.card_emojis = {
            'Ace': ':regional_indicator_a:',
            '2': ':two:',
            '3': ':three:',
            '4': ':four:',
            '5': ':five:',
            '6': ':six:',
            '7': ':seven:',
            '8': ':eight:',
            '9': ':nine:',
            '10': ':keycap_ten:',
            'Jack': ':regional_indicator_j:',
            'Queen': ':regional_indicator_q:',
            'King': ':regional_indicator_k:',
            'Spades': ':spades:',
            'Clubs': ':clubs:',
            'Diamonds': ':diamonds:',
            'Hearts': ':heart:'
        }

        self.BLACKJACK = ['blackjack', 'bj']
        self.VALID_GAMES = [] + self.BLACKJACK
        self.INVALID_GAME = 'Invalid Game'

        self.HIT = '☝️'
        self.HOLD = '🛑'
        self.DOUBLE = '🇩'
        self.SPLIT = '🇸'
        self.AGAIN = '🔄'

        self.DECK_SHUFFLED = '{} Deck Shuffled'
        self.BJ_NUM_DECKS = 4
        self.BJ_SHUFFLED_THRESHOLD = (52 * self.BJ_NUM_DECKS) // 2

        self.USER_SETS_TABLE = 'user_sets'
        self.USER_SETS_TABLE_COL1 = ('id', bot.db.INTEGER)
        self.USER_SETS_TABLE_COL2 = ('black', bot.db.TEXT)
        self.USER_SETS_TABLE_COL3 = ('red', bot.db.TEXT)
        self.USER_SETS_TABLE_COL4 = ('suits', bot.db.TEXT)
        self.USER_SETS_TABLE_COL5 = ('back', bot.db.TEXT)
        self.USER_SETS_TABLE_COL6 = ('base', bot.db.TEXT)
        self.USER_SETS_TABLE_COL7 = ('border', bot.db.TEXT)

        self.USER_UNLOCKS_TABLE = 'user_unlocks'
        self.USER_UNLOCKS_TABLE_COL1 = ('id', bot.db.INTEGER)
        self.USER_UNLOCKS_TABLE_COL2 = ('unlocks', bot.db.TEXT)

        self.STATS_DB = os.path.join(os.path.dirname(__file__), 'gamez', 'stats.db')
        self.CARD_DB = os.path.join(os.path.dirname(__file__), 'gamez', 'cards.db')

        self.CUSTOM_SET = 'custom'
        self.SET_SHOP = {
            self.CUSTOM_SET: 50000,
            card_imgs.JAPAN_SET: 20000,
        }

        self.bot = bot
        self.bj_deck = deck.Deck(num_decks=self.BJ_NUM_DECKS)
        self.bj_states = {}
        self.cr = credits.Credits(bot)
        self.stats = stats.Stats(self.bot, self.STATS_DB, self.VALID_GAMES)
        self._init_card_db()

        self.filehost_url = None
        self.filehost_key = None
        try:
            with open(os.path.join(os.path.dirname(__file__), 'gamez', 'filehost')) as f:
                lines = f.readlines()
                self.filehost_url = lines[0]
                self.filehost_key = lines[1]
        except Exception as e:
            self.bot.vars.LOGGER.error('Invalid filehost file')
        self.filehost = self.filehost_key and self.filehost_url

    def _init_card_db(self):
        self.card_db = self.bot.db.DB(self.CARD_DB)
        if not self.card_db.create_table(self.USER_SETS_TABLE, self.USER_SETS_TABLE_COL1, self.USER_SETS_TABLE_COL2, self.USER_SETS_TABLE_COL3,
                                         self.USER_SETS_TABLE_COL4, self.USER_SETS_TABLE_COL5, self.USER_SETS_TABLE_COL6, self.USER_SETS_TABLE_COL7):
            self.bot.vars.LOGGER.error(f'Could not create {self.USER_SETS_TABLE} table.')
        if not self.card_db.create_table(self.USER_UNLOCKS_TABLE, self.USER_UNLOCKS_TABLE_COL1, self.USER_UNLOCKS_TABLE_COL2):
            self.bot.vars.LOGGER.error(f'Could not create {self.USER_UNLOCKS_TABLE} table.')

    def _upload_image(self, img, itype='PNG'):
        with BytesIO() as img_bytes:
            img.save(img_bytes, format=itype)
            img_b64 = base64.b64encode(img_bytes.getvalue())
            data = {
                'API_KEY': self.filehost_key,
                'base64': str(img_b64)[2:],
                'ext': 'png'
            }
            r = requests.post(self.filehost_url, json=data)
            if r.status_code == 200:
                return r.json()['url'].replace('http://', 'https://')
        return None

    def _get_user_card_sheet(self, user, blank=False):
        return card_imgs.load_sheet_img(user.id, blank=blank)

    def _get_user_card_settings(self, user):
        settings = {
            'black': card_imgs.DEFAULT_SET,
            'red': card_imgs.DEFAULT_SET,
            'suits': card_imgs.DEFAULT_SET,
            'back': card_imgs.DEFAULT_SET,
            'base': card_imgs.DEFAULT_SET,
            'border': card_imgs.DEFAULT_SET
        }
        row = self.card_db.get_row(self.USER_SETS_TABLE, (self.USER_SETS_TABLE_COL1[0], user.id), factory=True)
        if row:
            settings['black'] = row['black']
            settings['red'] = row['red']
            settings['suits'] = row['suits']
            settings['back'] = row['back']
            settings['base'] = row['base']
            settings['border'] = row['border']
        return settings

    def _save_user_card_settings(self, user, settings):
        row = self.card_db.get_row(self.USER_SETS_TABLE, (self.USER_SETS_TABLE_COL1[0], user.id), factory=True)
        if row:
            self.card_db.update_row(self.USER_SETS_TABLE,
                                    (self.USER_SETS_TABLE_COL1[0], user.id),
                                    (self.USER_SETS_TABLE_COL2[0], settings['black']),
                                    (self.USER_SETS_TABLE_COL3[0], settings['red']),
                                    (self.USER_SETS_TABLE_COL4[0], settings['suits']),
                                    (self.USER_SETS_TABLE_COL5[0], settings['back']),
                                    (self.USER_SETS_TABLE_COL6[0], settings['base']),
                                    (self.USER_SETS_TABLE_COL7[0], settings['border']))
        else:
            self.card_db.insert_row(self.USER_SETS_TABLE,
                                    (self.USER_SETS_TABLE_COL1[0], user.id),
                                    (self.USER_SETS_TABLE_COL2[0], settings['black']),
                                    (self.USER_SETS_TABLE_COL3[0], settings['red']),
                                    (self.USER_SETS_TABLE_COL4[0], settings['suits']),
                                    (self.USER_SETS_TABLE_COL5[0], settings['back']),
                                    (self.USER_SETS_TABLE_COL6[0], settings['base']),
                                    (self.USER_SETS_TABLE_COL7[0], settings['border']))
        sheet, sheet_blank = card_imgs.make_sheet_img(base_name=settings['base'],
                                                      border_name=settings['border'],
                                                      suits_name=settings['suits'],
                                                      black_name=settings['black'],
                                                      red_name=settings['red'],
                                                      back_name=settings['back'])
        sheet.save(os.path.join(card_imgs.CUSTOM_PATH, f'{user.id}.png'), format='PNG')
        sheet_blank.save(os.path.join(card_imgs.CUSTOM_PATH, f'{user.id}_blank.png'), format='PNG')

    def _get_user_unlocks(self, user):
        row = self.card_db.get_row(self.USER_UNLOCKS_TABLE, (self.USER_UNLOCKS_TABLE_COL1[0], user.id), factory=True)
        if row:
            return json.loads(base64.b64decode(row['unlocks']))
        return []

    def _set_user_unlocks(self, user, unlocks):
        row = self.card_db.get_row(self.USER_UNLOCKS_TABLE, (self.USER_UNLOCKS_TABLE_COL1[0], user.id), factory=True)
        data = str(base64.b64encode(json.dumps(unlocks).encode()))[2:-1]
        if row:
            self.card_db.update_row(self.USER_UNLOCKS_TABLE,
                                    (self.USER_UNLOCKS_TABLE_COL1[0], user.id),
                                    (self.USER_UNLOCKS_TABLE_COL2[0], data))
        else:
            self.card_db.insert_row(self.USER_UNLOCKS_TABLE,
                                    (self.USER_UNLOCKS_TABLE_COL1[0], user.id),
                                    (self.USER_UNLOCKS_TABLE_COL2[0], data))

    """
    Blackjack Methods
    """

    async def _check_bj_deck(self, ctx):
        self.bj_deck.lock.acquire()
        if self.bj_deck.cards_used() >= self.BJ_SHUFFLED_THRESHOLD:
            self.bj_deck.reshuffle()
            await ctx.send(self.DECK_SHUFFLED.format('Blackjack'))
        self.bj_deck.lock.release()

    def _cards_to_embed_str(self, cards):
        return ' '.join([self.card_emojis[c] for c in cards])

    def _cards_to_embed_sum_name(self, user, cards, unknown=False):
        return f'{user.name} ({bj.best_sum(cards)}{"?" if unknown else ""})'

    def _embed_from_bj(self, user, blackjack, sheet_img):
        user_creds = self.cr.get_user_creds(user)
        bets = blackjack.bets
        bot_hand = blackjack.house_cards
        user_hands = blackjack.player_cards
        results = blackjack.states

        roles = [r for r in sorted(user.roles, reverse=True) if not r.name == '@everyone']
        e = Embed(colour=roles[0].colour if roles else 0)

        e.title = 'Blackjack'
        e.description = f'Credits: {user_creds}\nCurrent Bets: {"/".join([str(b) for b in bets])}'
        e.set_thumbnail(url=user.avatar_url)

        img_url = ''
        if self.filehost:
            bj_img = card_imgs.make_bj_img(self, user, blackjack, sheet_img)
            img_url = self._upload_image(bj_img)
            if img_url:
                e.set_image(url=img_url)
        if (not self.filehost) or (self.filehost and not img_url):
            name = ''
            value = ''
            if blackjack.get_curr_state() != bj.ONGOING:
                name = self._cards_to_embed_sum_name(self.bot.user, bot_hand)
                value = self._cards_to_embed_str(bot_hand)
            else:
                name = self._cards_to_embed_sum_name(self.bot.user, bot_hand[:1], unknown=True)
                value = self._cards_to_embed_str(bot_hand[:1]) + ':grey_question:'
            e.add_field(name=name, value=value)

            name = f'{user.name} ({"/".join([str(bj.best_sum(c)) for c in user_hands])})'
            val = ''
            for i in range(len(user_hands)):
                if i == blackjack.curr_hand and len(user_hands) > 1:
                    val += ':white_small_square:'
                val += self._cards_to_embed_str(user_hands[i]) + '\n'
            e.add_field(name=name, value=val)

        result_values = []
        if blackjack.get_curr_state() == bj.PLAYER_DONE:
            for i in range(len(results)):
                r = results[i]
                if r != bj.ONGOING:
                    if r == bj.PLAYER_WIN:
                        result_values += ['You Won!']
                    elif r == bj.PLAYER_LOSE:
                        result_values += ['You Lost!']
                    elif r == bj.PLAYER_BUST:
                        result_values += ['You Busted!']
                    elif r == bj.HOUSE_BUST:
                        result_values += ['Bot Busted, You Won!']
                    elif r == bj.DRAW:
                        result_values += ['Draw!']
        if result_values:
            value = '\n'.join(result_values)
            net = blackjack.net
            if net >= 0:
                net = f'+{net}'
            e.description = f'Bets: {"/".join([str(b) for b in bets])}'
            e.set_footer(text=f'Results:\n{value}\n\nCredits: {user_creds}({net})',
                         icon_url=self.bot.utils.make_data_url(user))
        else:
            e.set_footer(text=f'{user.id}',
                         icon_url=self.bot.utils.make_data_url(user))

        return e

    async def _finalize_bj(self, user, msg, embed):
        bj_state, _ = self.bj_states.pop(user.id, None)
        await msg.clear_reactions()
        if self.cr.get_user_creds(user) >= BJ_DEFAULT_BET:
            await msg.add_reaction(self.AGAIN)
        return bj_state

    @commands.Cog.listener()
    async def on_reaction_add(self, reaction, user):
        if user == self.bot.user or not reaction.message.embeds:
            return
        msg = reaction.message
        embed = msg.embeds[0]
        ctx = await self.bot.get_context(msg)
        if not self.bot.user in [u async for u in reaction.users()]:
            return
        if not embed:
            return
        data = self.bot.utils.get_embed_data(embed)
        if not data:
            return

        if not data['user_id'] == user.id:
            return

        if embed.title and embed.title == 'Blackjack':
            if reaction.emoji in [self.HIT, self.HOLD, self.DOUBLE, self.SPLIT]:
                bj_game, sheet_img = self.bj_states[user.id]
                bj_state_final = None
                lock = bj_game.lock
                if not lock.acquire(blocking=False):
                    return
                try:
                    action = reaction.emoji
                    error = None
                    try:
                        await self._check_bj_deck(ctx)
                        if action == self.HIT:
                            bj_game.hit()
                        elif action == self.DOUBLE:
                            bj_game.double()
                        elif action == self.HOLD:
                            bj_game.hold()
                        elif action == self.SPLIT:
                            bj_game.split()
                    except bj.BlackjackException as e:
                        error = e

                    if bj_game.get_curr_state() == bj.PLAYER_DONE:
                        bj_state_final = await self._finalize_bj(user, msg, embed)
                    else:
                        await msg.remove_reaction(reaction, user)
                    e = self._embed_from_bj(user, bj_game, sheet_img)
                    if error:
                        e.add_field(name='Error', value=error, inline=False)
                    await msg.edit(embed=e)
                finally:
                    lock.release()
                if bj_state_final:
                    await self.stats.save_bj_stats(bj_state_final, user.id)
            elif reaction.emoji == self.AGAIN:
                ctx.author = user
                await self.blackjack(ctx)
        elif 'buy' in data and reaction.emoji == self.bot.utils.CONFIRM_EMOJI:
            choice = data['buy']
            price = data['price']
            user_unlocks = self._get_user_unlocks(user)
            user_creds = self.cr.get_user_creds(user)
            if user_creds >= price and not choice in user_unlocks:
                if choice != self.CUSTOM_SET:
                    user_unlocks += [choice]
                self.cr.transfer_from_to(user, self.bot.user, price)
                self._set_user_unlocks(user, user_unlocks)
                await msg.add_reaction('👍')
        elif 'settings' in data and reaction.emoji == self.bot.utils.CONFIRM_EMOJI:
            settings = data['settings']
            self._save_user_card_settings(user, settings)
            await msg.add_reaction('👍')

    @commands.command(aliases=['creds', 'cred', 'cr', 'balance', 'bal'],
                      description='Check Credits from Games',
                      help='Check own credits, or mention another user to see theirs',
                      brief='Check Credits')
    async def credits(self, ctx, user: User = None):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        if not user:
            user = ctx.author
        creds = self.cr.get_user_creds(user)

        e = Embed(colour=Colour.green())
        e.title = 'Games Balance'
        e.description = 'Credits: ' + str(creds)
        e.set_thumbnail(url=user.avatar_url)

        await ctx.send(embed=e)

    @commands.command(description='See credit amount in the "bank"',
                      help='Requires owner permission.',
                      brief='Check "bank"')
    async def bank(self, ctx):
        ctx.author = self.bot.user
        await self.credits(ctx)

    @commands.command(description='Send credits to another user',
                      help='Must mention user to send credits.',
                      brief='Send Credits')
    async def send(self, ctx, user: User, amount: int):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        if amount < 0:
            return await ctx.send('You cannot send negative credits.')
        author_creds = self.cr.get_user_creds(ctx.author)
        e = Embed(colour=Colour.green())
        if author_creds >= amount:
            self.cr.transfer_from_to(ctx.author, user, amount)
            e.description = f'Sent {amount} credit(s) to {user.mention}'
        else:
            e.description = 'Insufficient credit(s) to send.'

        await ctx.send(embed=e)

    @commands.command(description='Transfer credits from one user to another user',
                      help='Requires owner permission.',
                      brief='Transfer Credits')
    @commands.is_owner()
    async def transfer(self, ctx, user1: User, user2: User, amount: int):
        ctx.author = user1
        await self.send(ctx, user2, amount)

    @commands.command(aliases=['bj'],
                      description='Play Blackjack with Eschamali',
                      help='Play Blackjack with Eschamali.\n☝️ = Hit\n🛑 = Hold\n🇩 = Double Bet and +1 Card',
                      brief='Play Blackjack')
    async def blackjack(self, ctx, bet: int = BJ_DEFAULT_BET):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        user = ctx.author
        if user.id in self.bj_states.keys():
            return await ctx.send('You already have a Blackjack game running.')
        try:
            await self._check_bj_deck(ctx)
            bj_game = bj.Blackjack(self.cr, self.bj_deck, bet, self.bot.user, user)
            sheet_img = self._get_user_card_sheet(user, blank=True)
            self.bj_states[user.id] = (bj_game, sheet_img)
        except bj.BlackjackException as e:
            return await ctx.send(e)

        msg = await ctx.send(embed=self._embed_from_bj(user, bj_game, sheet_img))
        if bj_game.get_curr_state() == bj.ONGOING:
            await msg.add_reaction(self.HIT)
            await msg.add_reaction(self.HOLD)
            await msg.add_reaction(self.DOUBLE)
            await msg.add_reaction(self.SPLIT)
        else:
            bj_state_final = await self._finalize_bj(user, msg, msg.embeds[0])
            await self.stats.save_bj_stats(bj_state_final, user.id)

    @commands.command(aliases=['gst', 'st'],
                      description='Check stats for a game',
                      help=VALID_GAMES_HELP,
                      brief='Game Stats')
    async def game_stats(self, ctx, game):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        if game in self.BLACKJACK:
            doubled, splits, busts, nums = await self.stats.get_bj_global_stats()
            e = Embed(colour=0)
            line_template = '{:^3}|{:^8}|{:^10}|{:^9}|{:^11}|{:^6}'
            text = '```' + line_template.format('Num', 'Bot Wins', 'Bot Losses', 'User Wins', 'User Losses', 'Draws') + '\n'
            for r in nums:
                text += line_template.format(r[0], r[1], r[2], r[3], r[4], r[5]) + '\n'
            text += '```'

            busts_value = '```'
            busts_value += f'Bot Busts : {busts[2]}\n'
            busts_value += f'User Busts: {busts[4]}\n'
            busts_value += '```'

            doubled_value = '```'
            doubled_value += f'User Wins  : {doubled[3]}\n'
            doubled_value += f'User Losses: {doubled[4]}\n'
            doubled_value += f'User Draws : {doubled[5]}\n'
            doubled_value += '```'

            splits_value = '```'
            splits_value += f'User Wins  : {splits[3]}\n'
            splits_value += f'User Losses: {splits[4]}\n'
            splits_value += f'User Draws : {splits[5]}\n'
            splits_value += '```'

            bot_wins = busts[4] + sum([r[1] for r in nums])
            bot_losses = busts[2] + sum([r[2] for r in nums])
            bot_draws = sum([r[5] for r in nums])
            total = bot_wins + bot_losses + bot_draws
            win_p = (bot_wins / total * 100) if total else 0
            loss_p = (bot_losses / total * 100) if total else 0
            draw_p = (bot_draws / total * 100) if total else 0

            win_loss = '```'
            win_loss += 'Wins  : {:7} ({:.2f}%)\n'.format(bot_wins, win_p)
            win_loss += 'Losses: {:7} ({:.2f}%)\n'.format(bot_losses, loss_p)
            win_loss += 'Draws : {:7} ({:.2f}%)\n'.format(bot_draws, draw_p)
            win_loss += 'Total : {:7}'.format(total)
            win_loss += '```'

            e.title = 'Blackjack Global Stats'
            e.description = text
            e.add_field(name='Busts', value=busts_value)
            e.add_field(name='\u200b', value='\u200b')
            e.add_field(name='Doubles', value=doubled_value)
            e.add_field(name='Splits', value=splits_value)
            e.add_field(name='\u200b', value='\u200b')
            e.add_field(name='Bot Win/Loss', value=win_loss)
            await ctx.send(embed=e)

    @commands.command(aliases=['pst'],
                      description='Check personal stats for a game',
                      help=VALID_GAMES_HELP,
                      brief='Personal Game Stats')
    async def personal_stats(self, ctx, game, user: Member = None):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        if not user:
            user = ctx.author
        if game in self.BLACKJACK:
            wins, losses, draws = await self.stats.get_bj_personal_stats(user)

            total = wins + losses + draws
            win_p = (wins / total * 100) if total else 0
            loss_p = (losses / total * 100) if total else 0
            draw_p = (draws / total * 100) if total else 0

            win_loss = '```'
            win_loss += 'Wins  : {:7} ({:.2f}%)\n'.format(wins, win_p)
            win_loss += 'Losses: {:7} ({:.2f}%)\n'.format(losses, loss_p)
            win_loss += 'Draws : {:7} ({:.2f}%)\n'.format(draws, draw_p)
            win_loss += 'Total : {:7}'.format(total)
            win_loss += '```'

            roles = [r for r in sorted(user.roles, reverse=True) if not r.name == '@everyone']
            e = Embed(colour=roles[0].colour if roles else 0)
            e.title = 'Blackjack Personal Stats'
            e.description = win_loss
            await ctx.send(embed=e)

    """
    Card Customization Methods
    """

    @commands.command(description='Card Set Shop',
                      help='Just use it.',
                      brief='Set Shop')
    async def shop(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        user_unlocks = self._get_user_unlocks(ctx.author)
        e = Embed()
        e.title = 'Card Set Shop'
        e.description = '```'
        for name, price in self.SET_SHOP.items():
            price_text = f'{price}'
            if name in user_unlocks:
                price_text = 'BOUGHT'
            e.description += '{:10} : {:>6}\n'.format(name.capitalize(), price_text)
        e.description += '```'

        await ctx.send(embed=e)

    @commands.command(description='Buy Card Set from the shop',
                      help='Use the shop command to see what is buyable',
                      brief='Buy Sets')
    async def buy(self, ctx, choice):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        choice = choice.lower()
        user_unlocks = self._get_user_unlocks(ctx.author)
        if choice in user_unlocks:
            return await ctx.send('You have already bought that set!')
        if choice in self.SET_SHOP:
            set_price = self.SET_SHOP[choice]
            data = {
                'buy': choice,
                'price': set_price
            }

            e = Embed()
            e.description = f'`{choice.capitalize()}` Card Set'
            e.set_footer(icon_url=self.bot.utils.make_data_url(ctx.author, data), text=f'Buy for {set_price} credits?')
            m = await ctx.send(embed=e)
            return await m.add_reaction(self.bot.utils.CONFIRM_EMOJI)
        return await ctx.send('Invalid Set.')

    @commands.command(aliases=['prv'],
                      description='Preview Card Sets',
                      help='No argument previews current cards image\nArgument previews a set',
                      brief='Preview Sets')
    async def preview(self, ctx, card_set=''):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        card_set = card_set.lower()
        sheet = None
        sheet_blank = None
        if card_set == self.CUSTOM_SET:
            return await ctx.send('Ask a bot owner to make a custom card set.')
        elif card_set and card_set in self.SET_SHOP:
            sheet, sheet_blank = card_imgs.make_set_sheet_img(card_set)
        else:
            sheet = self._get_user_card_sheet(ctx.author)
            sheet_blank = self._get_user_card_sheet(ctx.author, blank=True)

        if sheet and sheet_blank:
            sheet_url = self._upload_image(sheet)
            sheet_blank_url = self._upload_image(sheet_blank)
            if sheet_url and sheet_blank_url:
                await ctx.send(sheet_url)
                return await ctx.send(sheet_blank_url)

    @commands.command(aliases=['set'],
                      description='Set different parts of your cards to other set parts.',
                      help='Valid Parts: base, border, suits, black, red, back',
                      brief='Set Card Parts')
    async def set_part(self, ctx, card_part, set_name):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        card_part = card_part.lower()
        set_name = set_name.lower()
        if set_name in card_imgs.SETS:
            user_unlocks = self._get_user_unlocks(ctx.author)
            if not set_name in user_unlocks:
                return await ctx.send('You have not bought that set!')

            settings = self._get_user_card_settings(ctx.author)
            if card_part == 'all':
                for k, _ in settings.items():
                    settings[k] = set_name
            elif card_part in settings:
                settings[card_part] = set_name
            else:
                return await ctx.send('Invalid card part.')

            sheet, sheet_blank = card_imgs.make_sheet_img(settings['base'], settings['border'], settings['suits'],
                                                          settings['black'], settings['red'], settings['back'])
            sheet_url = self._upload_image(sheet)
            sheet_blank_url = self._upload_image(sheet_blank)
            data = {'settings': settings}

            e = Embed()
            e.set_image(url=sheet_url)
            e.set_thumbnail(url=sheet_blank_url)
            e.set_footer(icon_url=self.bot.utils.make_data_url(ctx.author, data),
                         text=f'Change Card "{card_part}"?')

            m = await ctx.send(embed=e)
            return await m.add_reaction(self.bot.utils.CONFIRM_EMOJI)
        return await ctx.send('Invalid card set.')

    @commands.command(description='Set different parts of your cards to other set parts.',
                      help='Valid Parts: base, border, suits, black, red, back',
                      brief='Set Card Parts')
    async def settings(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        settings = self._get_user_card_settings(ctx.author)
        text = '```'
        for k, v in settings.items():
            text += '{:7} : {:7}\n'.format(k.capitalize(), v)
        e = Embed()
        e.title = 'Card Settings'
        e.set_thumbnail(url=ctx.author.avatar_url)
        e.description = text + '```'
        return await ctx.send(embed=e)

    """
    Other Game Methods
    """
    @commands.command(aliases=['dk'],
                      description='See Info for Game Deck',
                      help=VALID_GAMES_HELP,
                      brief='Check Game Deck')
    async def deck(self, ctx, *, game):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        e = Embed(colour=Colour.from_rgb(255, 255, 254))
        title = '{} Deck Info'
        if game in self.BLACKJACK:
            title = title.format('Blackjack')
            self.bj_deck.lock.acquire()
            e.description = ''
            e.description += '```Cards Used: {:3}\n'.format(self.bj_deck.cards_used())
            e.description += 'Cards Left: {:3}```'.format(self.bj_deck.cards_left())
            self.bj_deck.lock.release()
        else:
            return await ctx.send(self.INVALID_GAME)
        e.title = title
        await ctx.send(embed=e)

    @commands.command(aliases=['d'],
                      description='Get Daily Credits for Games',
                      help='24h Reset',
                      brief='Credits Daily')
    async def daily(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        user = ctx.author
        daily_check = self.cr.daily(user)

        e = Embed(colour=Colour.green())
        e.title = 'Games Daily'
        if daily_check is True:
            e.description = f'You have gained 1000 Credits!\nCredits: {self.cr.get_user_creds(user)}'
        else:
            e.description = f'Time until daily - {daily_check}'
        await ctx.send(embed=e)

    @commands.command(aliases=['leader', 'board', 'lb'],
                      description='Credits Leaderboard',
                      help='None',
                      brief='Leaderboard')
    async def leaderboard(self, ctx, page=1):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        e = Embed(colour=Colour.green())
        e.title = 'Credit Leaderboard'

        rows = self.cr.get_all_user_creds()
        rows = [r for r in rows if r[0] != self.bot.user.id]
        max_pages = len(rows) // 10 + 1
        page = max_pages if page > max_pages else page
        rows.sort(key=lambda r: r[1], reverse=True)
        rows = rows[10 * (page - 1):10 * page]
        line_template = '{:^8}|{:^11}|{:<20}\n'
        rank = 10 * (page - 1) + 1
        text = '```' + line_template.format('Rank', 'Credits', 'Name')
        for r in rows:
            user = None
            for g in self.bot.guilds:
                user = g.get_member(r[0])
                if user:
                    break
            text += line_template.format(rank, r[1], f'{user.name[0:15]}#{user.discriminator}' if user else r[0])
            rank += 1
        text += '```'

        e.description = text
        e.set_footer(text=f'{page}/{max_pages}')

        await ctx.send(embed=e)

    @commands.command(aliases=['gs'],
                      description='Guess a number from 1-10',
                      help='Guess a number between 1-10\nExact = 100\nWithin 1 = 50\nWithin 2 = 10\n4 or more = -20',
                      brief='Guess Number Game')
    async def guess(self, ctx, guess: int):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        low = 1
        high = 10
        if not (0 < guess <= 10):
            return await ctx.send('Number must be 1-10')
        num = random.randint(low, high)
        diff = abs(guess - num)
        e = Embed(colour=Colour.greyple())
        e.set_thumbnail(url=ctx.author.avatar_url)

        e.add_field(name='Bot Number', value=num)
        e.add_field(name='Your Guess', value=guess)
        gain = 0
        if diff == 0:
            gain = 100
        elif diff == 1:
            gain = 50
        elif diff == 2:
            gain = 10
        elif diff >= 4:
            gain = -20
        creds = self.cr.get_user_creds(ctx.author)
        if (creds + gain) > 0:
            self.cr.transfer_from_to(self.bot.user, ctx.author, gain)
        e.set_footer(text=f'Credits: {self.cr.get_user_creds(ctx.author)}({"+" if gain > 0 else ""}{gain})')
        e.title = 'Guess Game'

        await ctx.send(embed=e)

    @commands.command(aliases=['8', '8ball', '8b'],
                      description='Ask the magic eight-ball a *question*',
                      help='Luck doesn\'t need help',
                      brief='Ask 8ball')
    async def eightball(self, ctx, *, question):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        await ctx.send(embed=Embed(
            title=':8ball:The Magical 8-Ball:8ball:'
        ).add_field(
            name=':question:Question:question:',
            value=question,
            inline=False
        ).add_field(
            name=':exclamation:Answer:exclamation:',
            value=self.ANSWERS[random.randint(0, len(self.ANSWERS) - 1)],
            inline=False))

    @commands.command(description='Choose a choice from given *choices*',
                      help='*choices* are separated by ";"\ni.e choice 1;choice 2;etc...',
                      brief='Choose choice')
    async def choose(self, ctx, *, choices):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        choices = re.split(';|\n', choices)
        chosen = choices[random.randint(0, len(choices) - 1)]
        await ctx.send(embed=Embed(
            title='The Chooser'
        ).add_field(
            name='Choices',
            value='\n'.join(choices),
            inline=False
        ).add_field(
            name='Answer',
            value=chosen,
            inline=False))

    async def determine_rps_winner(self, ctx, author_choice):
        bot_choice = random.randint(0, 2)
        if author_choice == bot_choice:
            winner = None
        elif ((author_choice == 0 and bot_choice == 1) or
              (author_choice == 1 and bot_choice == 2) or
              (author_choice == 2 and bot_choice == 0)):
            winner = ctx.author
        else:
            winner = self.bot.user

        await ctx.send(embed=Embed(
            title='Rock, Paper, Scissors',
            description=f'{ctx.author.mention} vs. {ctx.bot.user.mention}'
        ).add_field(
            name='Battle',
            value=f'{self.PICKS[author_choice]} vs. {self.PICKS[bot_choice]}'
        ).add_field(
            name='Winner',
            value=f'{winner.mention if winner else "Tie."}'))

    @commands.group(description='Play Rock, Paper, Scissors with the bot',
                    help='Choose rock, paper, or scissors\nAliased by r, p, and s',
                    brief='R,P,S')
    async def rps(self, ctx):
        if not self.bot.utils.can_cog_in(self, ctx.channel) or ctx.invoked_subcommand is None:
            return

    @rps.command(aliases=['r'],
                 description=':rock:')
    async def rock(self, ctx):
        await self.determine_rps_winner(ctx, 0)

    @rps.command(aliases=['p'],
                 description=':newspaper:')
    async def paper(self, ctx):
        await self.determine_rps_winner(ctx, 1)

    @rps.command(aliases=['s'],
                 description=':scissors:')
    async def scissors(self, ctx):
        await self.determine_rps_winner(ctx, 2)

    @commands.command(aliases=['r'],
                      description='Roll a number from 1 to *max_num*',
                      help='Default *max_num* is 100',
                      brief='Roll a number')
    async def roll(self, ctx, max_num: int = 100):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        await ctx.send(f'`{random.randint(1, max_num)}`')

    @commands.command(description='Make a poll from *options*',
                      help='*options* are separated by ";"\nFirst option in list is set as the question/title',
                      brief='Make poll')
    async def poll(self, ctx, *, options):
        if not self.bot.utils.can_cog_in(self, ctx.channel):
            return
        options = re.split(';|\n', options)
        poll_name = options[0]
        options = options[1:]
        if not (0 < len(options) <= 26):
            return
        a = '🇦'
        codepoint = ord(a)
        desc = ''
        for o in options:
            desc += f'{chr(codepoint)} {o}\n'
            codepoint += 1
        message = await ctx.send(embed=Embed(
            title=poll_name.strip(),
            description=desc))

        codepoint = ord(a)
        for o in options:
            await message.add_reaction(chr(codepoint))
            codepoint += 1


def setup(bot):
    bot.add_cog(Games(bot))
