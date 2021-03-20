import os
import importlib
import random
import re
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


HIT = '👋'
HOLD = '🛑'
ONE = '1️⃣'
FIVE = '5️⃣'
TEN = '🔟'
HUNDRED = '💯'
RAISE = '⏫'

GAMES_TABLE = 'games'
GAMES_TABLE_COL1 = ('discord_id', DB_MOD.INTEGER)
GAMES_TABLE_COL2 = ('credits', DB_MOD.INTEGER)
DB_PATH = os.path.join(os.path.dirname(__file__),  'games.db')


class Games(commands.Cog):
    """Fun commands"""

    def __init__(self, bot):
        self.bot = bot
        self._init_db()

    def _init_db(self):
        db = DB(DB_PATH)
        if not db.create_table(GAMES_TABLE, GAMES_TABLE_COL1, GAMES_TABLE_COL2):
            LOGGER.error('Could not create games table.')
        self._get_user_creds(self.bot.user, 100000)

    def _get_db(self):
        return DB(DB_PATH)

    """
    Blackjack Methods
    """

    def _get_user_creds(self, user, default=1000):
        db = self._get_db()
        creds = db.get_value(GAMES_TABLE, GAMES_TABLE_COL2[0], (GAMES_TABLE_COL1[0], user.id))
        if not creds:
            if not db.insert_row(GAMES_TABLE, (GAMES_TABLE_COL1[0], user.id), (GAMES_TABLE_COL2[0], default)):
                LOGGER.error(f'Could not create games entry for {user}({user.id})')
        return creds or default

    def _add_user_creds(self, user, amount):
        db = self._get_db()
        creds = db.get_value(GAMES_TABLE, GAMES_TABLE_COL2[0], (GAMES_TABLE_COL1[0], user.id))
        if not creds:
            creds = self._get_user_creds(user)
        creds += amount
        if not db.update_row(GAMES_TABLE, (GAMES_TABLE_COL1[0], user.id), (GAMES_TABLE_COL2[0], creds)):
            LOGGER.error(f'Could not update games entry for {user}({user.id})')

    def _make_deck(self, existing_cards=[], with_suits=False):
        nums = ['Ace', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'Jack', 'Queen', 'King']
        suits = ['Spades', 'Clubs', 'Diamonds', 'Hearts']
        deck = []
        for s in suits:
            for n in nums:
                if with_suits:
                    deck += [(n, s)]
                else:
                    deck += [n]
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

    def _get_bj_state(self, embed):
        fields = embed.fields
        bot_cards = fields[0].value.split('\n')[0]
        user_cards = fields[1].value.split('\n')[0]
        bot_cards = [emoji_cards[s] for s in bot_cards.split(' ')]
        user_cards = [emoji_cards[s] for s in user_cards.split(' ')]
        return {
            'bet': int(embed.description.split(' ')[3]),
            'bot': bot_cards,
            'user': user_cards,
            'deck': self._make_deck(bot_cards+user_cards)
        }

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
        card_sum = self._calc_sums(cards)
        card_sum = str(card_sum).replace(',', '') if len(card_sum) == 1 else str(card_sum).replace(',', '/').replace(' ', '')
        return f'{user.name} {card_sum}'

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
        e = reaction.emoji
        game_state = self._get_bj_state(embed)
        bet = game_state['bet']
        bot_cards = game_state['bot']
        user_cards = game_state['user']
        deck = game_state['deck']
        if e == HIT:
            user_cards += [deck.pop(0)]
            busted = self._is_busted(user_cards)
            # embed.set_field_at(1, name=embed.fields[1].name,
            #                    value=self._cards_to_embed_str(user_cards))
            embed.set_field_at(1, name=self._cards_to_embed_sum_name(user, user_cards),
                               value=self._cards_to_embed_str(user_cards))
            if busted:
                self._add_user_creds(self.bot.user, bet)
                embed.add_field(name='Result', value=f'You Busted!\nCredits: {self._get_user_creds(user)}', inline=False)
            if busted:
                await msg.clear_reactions()
            else:
                await msg.remove_reaction(reaction, user)
        elif e == HOLD:
            busted = self._is_busted(bot_cards)
            while not busted:
                new_bot_cards = bot_cards + [deck.pop(0)]
                busted = self._is_busted(new_bot_cards)
                if not busted:
                    bot_cards = new_bot_cards
            embed.set_field_at(0, name=self._cards_to_embed_sum_name(self.bot.user, bot_cards),
                               value=self._cards_to_embed_str(bot_cards))

            bot_sum = self._best_sum(bot_cards)
            user_sum = self._best_sum(user_cards)
            result_value = ''
            if bot_sum > user_sum:
                result_value = 'You Lost!'
                self._add_user_creds(self.bot.user, bet)
            elif bot_sum < user_sum:
                result_value = 'You Won!'
                self._add_user_creds(self.bot.user, -bet)
                self._add_user_creds(user, 2*bet)
            else:
                result_value = 'Draw!'
                self._add_user_creds(user, bet)
            result_value += f'\nCredits: {self._get_user_creds(user)}'
            embed.add_field(name='Result', value=result_value, inline=False)
            await msg.clear_reactions()
        elif e == RAISE:
            valid = [ONE, FIVE, TEN, HUNDRED]
            reactions = [r for r in msg.reactions if r.me and r.count > 1 and r.emoji in valid]
            amt = 0
            for r in reactions:
                users = await r.users().flatten()
                if user in users:
                    if r.emoji == ONE:
                        amt += 1
                    elif r.emoji == FIVE:
                        amt += 5
                    elif r.emoji == TEN:
                        amt += 10
                    elif r.emoji == HUNDRED:
                        amt += 100
            creds = self._get_user_creds(user)
            if amt > creds:
                await msg.channel.send(f'You do not have enough credits to bet {amt} more')
            else:
                bet += amt
                self._add_user_creds(user, -amt)
                desc = embed.description.split('\n')
                desc = [d.split(' ') for d in desc]
                desc[0][1] = str(self._get_user_creds(user))
                desc[1][2] = str(bet)
                desc = [' '.join(d) for d in desc]
                embed.description = '\n'.join(desc)
            await msg.remove_reaction(reaction, user)
        else:
            return
        await msg.edit(embed=embed)

    @commands.command(aliases=['creds', 'cred', 'cr', 'balance', 'bal'],
                      description='Play Blackjack with Eschamali',
                      help='Good luck',
                      brief='Play Blackjack')
    async def credits(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        user = ctx.author
        creds = self._get_user_creds(ctx.author)

        e = Embed(colour=Colour.green())
        e.title = 'Games Balance'
        e.description = 'Credits: ' + str(creds)
        e.set_thumbnail(url=user.avatar_url)

        await ctx.send(embed=e)

    @commands.command(description='See credit amount in the "bank"',
                      help='Requires bot ownage.',
                      brief='Check "bank"')
    @commands.is_owner()
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
            self._add_user_creds(user, amount)
            self._add_user_creds(ctx.author, -amount)
            e.description = f'Sent {amount} credit(s) to {user.mention}'
        else:
            e.description = f'Insufficient credit(s) to send.'

        await ctx.send(embed=e)

    @commands.command(description='Transfer credits from the bank to another user',
                      help='Requires bot ownage.',
                      brief='Transfer Credits')
    @commands.is_owner()
    async def transfer(self, ctx, user: User, amount: int):
        ctx.author = self.bot.user
        await self.send(ctx, user, amount)

    @commands.command(aliases=['bj'],
                      description='Play Blackjack with Eschamali',
                      help='Play Blackjack with Eschamali. Use reactions to hit/hold/raise. Using this command automatically deducts credits from your balance!!!',
                      brief='Play Blackjack')
    async def blackjack(self, ctx, bet: int = 100):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        user = ctx.author
        user_creds = self._get_user_creds(user)
        if user_creds < bet:
            return await ctx.send('You do not have enough credits for that bet.')
        deck = self._make_deck()
        bot_hand = [deck.pop(0), deck.pop(1)]
        user_hand = [deck.pop(0), deck.pop(0)]

        roles = [r for r in sorted(user.roles, reverse=True) if not r.name == '@everyone']

        e = Embed(colour=roles[0].colour if roles else 0)

        e.title = 'Blackjack'
        e.description = f'Credits: {user_creds - bet}\n' + f'Current Bet: {bet}'
        e.set_thumbnail(url=user.avatar_url)

        e.add_field(name=self._cards_to_embed_sum_name(self.bot.user, bot_hand),
                    value=self._cards_to_embed_str(bot_hand))
        e.add_field(name=self._cards_to_embed_sum_name(user, user_hand),
                    value=self._cards_to_embed_str(user_hand))

        e.set_footer(text=user.id)

        msg = await ctx.send(embed=e)
        self._add_user_creds(user, -bet)
        await msg.add_reaction(HIT)
        await msg.add_reaction(HOLD)
        await msg.add_reaction(ONE)
        await msg.add_reaction(FIVE)
        await msg.add_reaction(TEN)
        await msg.add_reaction(HUNDRED)
        await msg.add_reaction(RAISE)

    """
    Other Game Methods
    """

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
