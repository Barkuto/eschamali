import importlib
import os
import random
import re
import threading
from datetime import datetime, timedelta
from discord import Embed, User, Colour
from discord.ext import commands

UTILS = importlib.import_module('.utils', 'util')
DB_MOD = UTILS.DB_MOD
DB = DB_MOD.DB
LOGGER = UTILS.VARS.LOGGER

ANSWERS = [
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

PICKS = {
    0: ':rock:',
    1: ':newspaper:',
    2: ':scissors:'
}

card_emojis = {
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

emoji_cards = {v: k for k, v in card_emojis.items()}

NUM_DECKS = 4
HIT = '☝️'
HOLD = '🛑'
DOUBLE = '🇩'
SPLIT = '🇸'
AGAIN = '🔄'

ONGOING = 0
WIN = 1
LOSE = 2
USER_BUST = 3
BOT_BUST = 4
DRAW = 5

GAMES_TABLE = 'games'
GAMES_TABLE_COL1 = ('discord_id', DB_MOD.INTEGER)
GAMES_TABLE_COL2 = ('credits', DB_MOD.INTEGER)
DAILIES_TABLE = 'daily'
DAILIES_TABLE_COL1 = ('discord_id', DB_MOD.INTEGER)
DAILIES_TABLE_COL2 = ('timestamp', DB_MOD.INTEGER)
DB_PATH = os.path.join(os.path.dirname(__file__),  'games.db')


class Games(commands.Cog):
    """Fun commands"""

    def __init__(self, bot):
        self.bot = bot
        self._init_db()
        self.bj_states = {}

    def _init_db(self):
        db = DB(DB_PATH)
        if not db.create_table(GAMES_TABLE, GAMES_TABLE_COL1, GAMES_TABLE_COL2):
            LOGGER.error('Could not create games table.')
        if not db.create_table(DAILIES_TABLE, DAILIES_TABLE_COL1, DAILIES_TABLE_COL2):
            LOGGER.error('Could not create dailies table.')
        self._get_user_creds(self.bot.user, 100000)

    def _get_db(self):
        return DB(DB_PATH)

    """
    Blackjack Methods
    """

    def _get_user_creds(self, user, default=1000):
        db = self._get_db()
        creds = db.get_value(GAMES_TABLE, GAMES_TABLE_COL2[0], (GAMES_TABLE_COL1[0], user.id))
        if creds is None:
            if not db.insert_row(GAMES_TABLE, (GAMES_TABLE_COL1[0], user.id), (GAMES_TABLE_COL2[0], default)):
                LOGGER.error(f'Could not create games entry for {user}({user.id})')
            return default
        return creds

    def _add_user_creds(self, user, amount):
        db = self._get_db()
        creds = db.get_value(GAMES_TABLE, GAMES_TABLE_COL2[0], (GAMES_TABLE_COL1[0], user.id))
        if not creds:
            creds = self._get_user_creds(user)
        creds += amount
        if not db.update_row(GAMES_TABLE, (GAMES_TABLE_COL1[0], user.id), (GAMES_TABLE_COL2[0], creds)):
            LOGGER.error(f'Could not update games entry for {user}({user.id})')

    def _transfer_from_to(self, user1, user2, amount):
        self._add_user_creds(user1, -amount)
        self._add_user_creds(user2, amount)

    def _make_deck(self, existing_cards=[], with_suits=False, num_decks=1):
        nums = ['Ace', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'Jack', 'Queen', 'King']
        suits = ['Spades', 'Clubs', 'Diamonds', 'Hearts']
        deck = []
        for s in suits:
            for n in nums:
                if with_suits:
                    deck += [(n, s)] * num_decks
                else:
                    deck += [n] * num_decks
        for c in existing_cards:
            deck.remove(c)
        random.shuffle(deck)
        return deck

    def _calc_sums(self, cards):
        nums = []
        num_aces = 0
        for c in cards:
            if not c == 'Ace':
                if c in ['Jack', 'Queen', 'King']:
                    nums += [10]
                else:
                    nums += [int(c)]
            else:
                num_aces += 1
        base_sum = sum(nums)
        low_sum = base_sum
        high_sum = base_sum
        for _ in range(num_aces):
            low_sum += 1
        for _ in range(num_aces):
            if high_sum <= 10:
                high_sum += 11
            else:
                high_sum += 1
        if low_sum == high_sum:
            return (low_sum,)
        return (low_sum, high_sum)

    def _best_sum(self, cards):
        sums = self._calc_sums(cards)
        if len(sums) > 1 and sums[1] <= 21:
            return sums[1]
        return sums[0]

    def _determine_bj_winner(self, user, state):
        bet = state['bet']
        bot_cards = state['bot']
        user_cards = state['user']
        deck = state['deck']

        user_sum = self._best_sum(user_cards)
        bot_sum = self._best_sum(bot_cards)

        if not(user_sum == 21 and len(user_cards) == 2) and user_sum <= 21:
            while bot_sum < 17:
                bot_cards = bot_cards + [deck.pop(0)]
                bot_sum = self._best_sum(bot_cards)

        if user_sum > 21:
            state['result'] = USER_BUST
        elif bot_sum > 21:
            state['result'] = BOT_BUST
            self._transfer_from_to(self.bot.user, user, 2 * bet)
        elif bot_sum > user_sum:
            state['result'] = LOSE
        elif bot_sum < user_sum:
            state['result'] = WIN
            self._transfer_from_to(self.bot.user, user, 2 * bet)
        else:
            state['result'] = DRAW
            self._transfer_from_to(self.bot.user, user, bet)

        state['bot'] = bot_cards
        state['user'] = user_cards
        state['deck'] = deck
        return state

    def _is_busted(self, cards):
        sums = self._calc_sums(cards)
        busts = 0
        for s in sums:
            if s > 21:
                busts += 1
        if busts == len(sums):
            return True
        return False

    def _cards_to_embed_str(self, cards):
        return ' '.join([card_emojis[c] for c in cards])

    def _cards_to_embed_sum_name(self, user, cards):
        return f'{user.name} ({self._best_sum(cards)})'

    def _embed_from_bj_state(self, user, state):
        user_creds = self._get_user_creds(user)
        bet = state['bet']
        bot_hand = state['bot']
        user_hand = state['user']
        result = state['result']

        roles = [r for r in sorted(user.roles, reverse=True) if not r.name == '@everyone']
        e = Embed(colour=roles[0].colour if roles else 0)

        e.title = 'Blackjack'
        e.description = f'Credits: {user_creds}\n' + f'Current Bet: {bet}'
        e.set_thumbnail(url=user.avatar_url)

        e.add_field(name=self._cards_to_embed_sum_name(self.bot.user, bot_hand),
                    value=self._cards_to_embed_str(bot_hand))
        e.add_field(name=self._cards_to_embed_sum_name(user, user_hand),
                    value=self._cards_to_embed_str(user_hand))

        result_value = ''
        net = '0'
        if result == WIN:
            result_value = 'You Won!'
            net = f'+{bet}'
        elif result == LOSE:
            result_value = 'You Lost!'
            net = f'-{bet}'
        elif result == USER_BUST:
            result_value = 'You Busted!'
            net = f'-{bet}'
        elif result == BOT_BUST:
            result_value = 'Bot Busted, You Won!'
            net = f'+{bet}'
        elif result == DRAW:
            result_value = 'Draw!'
            net = '+0'
        if result_value:
            e.description = f'Bet: {bet}'
            e.add_field(name='Result', value=f'{result_value}\nCredits: {user_creds}({net})', inline=False)

        e.set_footer(text=user.id)

        return e

    async def _finalize_bj(self, user, msg, embed):
        self.bj_states.pop(user.id, None)
        await msg.clear_reactions()
        await msg.add_reaction(AGAIN)

    @commands.Cog.listener()
    async def on_reaction_add(self, reaction, user):
        if user == self.bot.user or not reaction.message.embeds:
            return
        msg = reaction.message
        embed = msg.embeds[0]
        if not embed.footer.text == str(user.id):
            return
        if not embed or not embed.title or not embed.title == 'Blackjack':
            return
        if not self.bot.user in [u async for u in reaction.users()]:
            return
        if reaction.emoji in [HIT, HOLD, DOUBLE, SPLIT]:
            state = self.bj_states[user.id]
            lock = state['lock']
            if not lock.acquire(blocking=False):
                return
            try:
                action = reaction.emoji
                split = state['split']
                clear = False
                if action in [HIT, DOUBLE]:
                    creds = self._get_user_creds(user)
                    if creds < state['bet'] and action == DOUBLE:
                        ctx = await self.bot.get_context(msg)
                        await msg.remove_reaction(reaction, user)
                        return await ctx.send('Insufficient credit(s) to double down.')
                    state['user'] += [state['deck'].pop(0)]
                    busted = self._is_busted(state['user'])
                    if action == DOUBLE:
                        self._transfer_from_to(user, self.bot.user, state['bet'])
                        state['bet'] *= 2
                        clear = True
                    if busted:
                        self.bj_states[user.id] = self._determine_bj_winner(user, state)
                        clear = True
                    else:
                        await msg.remove_reaction(reaction, user)
                        if action == DOUBLE:
                            self.bj_states[user.id] = self._determine_bj_winner(user, state)
                        elif action == HIT:
                            await msg.clear_reaction(DOUBLE)
                elif action == HOLD:
                    self.bj_states[user.id] = self._determine_bj_winner(user, state)
                    clear = True
                elif action == SPLIT:
                    pass
                if clear:
                    await self._finalize_bj(user, msg, embed)
                await msg.edit(embed=self._embed_from_bj_state(user, state))
            finally:
                lock.release()
        elif reaction.emoji == AGAIN:
            ctx = await self.bot.get_context(msg)
            ctx.author = user
            await self.blackjack(ctx)

    @commands.command(aliases=['creds', 'cred', 'cr', 'balance', 'bal'],
                      description='Play Blackjack with Eschamali',
                      help='Good luck',
                      brief='Play Blackjack')
    async def credits(self, ctx, user: User = None):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        if not user:
            user = ctx.author
        creds = self._get_user_creds(user)

        e = Embed(colour=Colour.green())
        e.title = 'Games Balance'
        e.description = 'Credits: ' + str(creds)
        e.set_thumbnail(url=user.avatar_url)

        await ctx.send(embed=e)

    @commands.command(description='See credit amount in the "bank"',
                      help='Requires bot ownage.',
                      brief='Check "bank"')
    async def bank(self, ctx):
        ctx.author = self.bot.user
        await self.credits(ctx)

    @commands.command(description='Send credits to another user',
                      help='Must mention user to send credits.',
                      brief='Send Credits')
    async def send(self, ctx, user: User, amount: int):
        if amount < 0:
            return await ctx.send('You cannot send negative credits.')
        author_creds = self._get_user_creds(ctx.author)
        e = Embed(colour=Colour.green())
        if author_creds >= amount:
            self._transfer_from_to(ctx.author, user, amount)
            e.description = f'Sent {amount} credit(s) to {user.mention}'
        else:
            e.description = 'Insufficient credit(s) to send.'

        await ctx.send(embed=e)

    @commands.command(description='Transfer credits from one user to another user',
                      help='Requires bot ownage.',
                      brief='Transfer Credits')
    @commands.is_owner()
    async def transfer(self, ctx, user1: User, user2: User, amount: int):
        ctx.author = user1
        await self.send(ctx, user2, amount)

    @commands.command(aliases=['bj'],
                      description='Play Blackjack with Eschamali',
                      help='Play Blackjack with Eschamali.\n☝️ = Hit\n🛑 = Hold\n🇩 = Double Bet and +1 Card',
                      brief='Play Blackjack')
    async def blackjack(self, ctx, bet: int = 100):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        user = ctx.author
        if user.id in self.bj_states.keys():
            return await ctx.send('You already have a Blackjack game running.')
        if bet <= 0:
            return await ctx.send('Invalid bet.')
        user_creds = self._get_user_creds(user)
        if user_creds < bet:
            return await ctx.send('You do not have enough credits for that bet.')
        deck = self._make_deck(num_decks=NUM_DECKS)
        bot_hand = [deck.pop(0)]
        user_hand = [deck.pop(0), deck.pop(0)]
        game_state = {
            'lock': threading.Lock(),
            'split': False,
            'bet': bet,
            'bot': bot_hand,
            'user': user_hand,
            'deck': self._make_deck(bot_hand + user_hand, num_decks=NUM_DECKS),
            'result': ONGOING
        }
        self.bj_states[user.id] = game_state
        self._transfer_from_to(user, self.bot.user, bet)

        user_sum = self._best_sum(user_hand)
        if user_sum == 21:
            self.bj_states[user.id] = self._determine_bj_winner(user, game_state)
            msg = await ctx.send(embed=self._embed_from_bj_state(user, self.bj_states[user.id]))
            await self._finalize_bj(user, msg, msg.embeds[0])
        else:
            msg = await ctx.send(embed=self._embed_from_bj_state(user, game_state))
            await msg.add_reaction(HIT)
            await msg.add_reaction(HOLD)
            await msg.add_reaction(DOUBLE)

    """
    Other Game Methods
    """
    @commands.command(aliases=['d'],
                      description='Get Daily Credits for Games',
                      help='24h Reset',
                      brief='Credits Daily')
    async def daily(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        user = ctx.author
        db = self._get_db()
        last_daily = db.get_value(DAILIES_TABLE, DAILIES_TABLE_COL2[0], (DAILIES_TABLE_COL1[0], user.id))
        e = Embed(colour=Colour.green())
        e.title = 'Games Daily'
        now = datetime.now().timestamp()
        give = False
        if not last_daily:
            give = True
            last_daily = now
            db.insert_row(DAILIES_TABLE, (DAILIES_TABLE_COL1[0], user.id), (DAILIES_TABLE_COL2[0], now))

        if give or (last_daily + (24 * 60 * 60)) - now <= 0:
            self._add_user_creds(user, 1000)
            e.description = f'You have gained 1000 Credits!\nCredits: {self._get_user_creds(user)}'
        else:
            diff = str(datetime.fromtimestamp(last_daily + (24 * 60 * 60)) - datetime.fromtimestamp(now))
            diff = diff.split('.')[0]
            e.description = f'Time until daily - {diff}'
        await ctx.send(embed=e)

    @commands.command(aliases=['leader', 'board', 'lb'],
                      description='Credits Leaderboard',
                      help='None',
                      brief='Leaderboard')
    async def leaderboard(self, ctx, page=1):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        db = self._get_db()

        e = Embed(colour=Colour.green())
        e.title = 'Credit Leaderboard'

        rows = db.get_all(GAMES_TABLE)
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
        if not UTILS.can_cog_in(self, ctx.channel):
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
        creds = self._get_user_creds(ctx.author)
        if (creds + gain) > 0:
            self._transfer_from_to(self.bot.user, ctx.author, gain)
        e.set_footer(text=f'Credits: {self._get_user_creds(ctx.author)}({"+" if gain > 0 else ""}{gain})')
        e.title = 'Guess Game'

        await ctx.send(embed=e)

    @commands.command(aliases=['8', '8ball', '8b'],
                      description='Ask the magic eight-ball a *question*',
                      help='Luck doesn\'t need help',
                      brief='Ask 8ball')
    async def eightball(self, ctx, *, question):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        await ctx.send(embed=Embed(
            title=':8ball:The Magical 8-Ball:8ball:'
        ).add_field(
            name=':question:Question:question:',
            value=question,
            inline=False
        ).add_field(
            name=':exclamation:Answer:exclamation:',
            value=ANSWERS[random.randint(0, len(ANSWERS) - 1)],
            inline=False))

    @commands.command(description='Choose a choice from given *choices*',
                      help='*choices* are separated by ";"\ni.e choice 1;choice 2;etc...',
                      brief='Choose choice')
    async def choose(self, ctx, *, choices):
        if not UTILS.can_cog_in(self, ctx.channel):
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
            value=f'{PICKS[author_choice]} vs. {PICKS[bot_choice]}'
        ).add_field(
            name='Winner',
            value=f'{winner.mention if winner else "Tie."}'))

    @commands.group(description='Play Rock, Paper, Scissors with the bot',
                    help='Choose rock, paper, or scissors\nAliased by r, p, and s',
                    brief='R,P,S')
    async def rps(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel) or ctx.invoked_subcommand is None:
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
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        await ctx.send(f'`{random.randint(1, max_num)}`')

    @commands.command(description='Make a poll from *options*',
                      help='*options* are separated by ";"\nFirst option in list is set as the question/title',
                      brief='Make poll')
    async def poll(self, ctx, *, options):
        if not UTILS.can_cog_in(self, ctx.channel):
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
