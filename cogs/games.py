import importlib
import os
import random
import re
import threading
from datetime import datetime, timedelta
from discord import Embed, User, Colour, Member
from discord.ext import commands

UTILS = importlib.import_module('.utils', 'util')
CREDITS = importlib.import_module('.credits', 'cogs.gamez')
BJ_MOD = importlib.import_module('.blackjack', 'cogs.gamez')
DECK = importlib.import_module('.deck', 'cogs.gamez')
STATS_MOD = importlib.import_module('.stats', 'cogs.gamez')

DB_MOD = UTILS.DB_MOD
DB = DB_MOD.DB
LOGGER = UTILS.VARS.LOGGER
Stats = STATS_MOD.Stats

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

BLACKJACK = ['blackjack', 'bj']
VALID_GAMES = [] + BLACKJACK
VALID_GAMES_HELP = 'Valid Games so far: "blackjack"'

HIT = '☝️'
HOLD = '🛑'
DOUBLE = '🇩'
SPLIT = '🇸'
AGAIN = '🔄'

BJ_DEFAULT_BET = 100
BJ_NUM_DECKS = 4
BJ_SHUFFLED_THRESHOLD = (52 * BJ_NUM_DECKS) / 2


class Games(commands.Cog):
    """Fun commands"""

    def __init__(self, bot):
        self.bot = bot
        self.bj_deck = DECK.Deck(num_decks=BJ_NUM_DECKS)
        self.bj_states = {}
        self.cr = CREDITS.Credits(bot.user)
        self.stats = Stats(self.bot, os.path.join(os.path.dirname(__file__), 'gamez', 'stats.db'))

    """
    Blackjack Methods
    """

    def _check_bj_deck(self):
        self.bj_deck.lock.acquire()
        if self.bj_deck.cards_used() >= BJ_SHUFFLED_THRESHOLD:
            self.bj_deck.reshuffle()
        self.bj_deck.lock.release()

    def _cards_to_embed_str(self, cards):
        return ' '.join([card_emojis[c] for c in cards])

    def _cards_to_embed_sum_name(self, user, cards, unknown=False):
        return f'{user.name} ({BJ_MOD.best_sum(cards)}{"?" if unknown else ""})'

    def _embed_from_bj(self, user, blackjack):
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

        name = ''
        value = ''
        if blackjack.get_curr_state() != BJ_MOD.ONGOING:
            name = self._cards_to_embed_sum_name(self.bot.user, bot_hand)
            value = self._cards_to_embed_str(bot_hand)
        else:
            name = self._cards_to_embed_sum_name(self.bot.user, bot_hand[:1], unknown=True)
            value = self._cards_to_embed_str(bot_hand[:1]) + ':grey_question:'
        e.add_field(name=name, value=value)

        name = f'{user.name} ({"/".join([str(BJ_MOD.best_sum(c)) for c in user_hands])})'
        val = ''
        for i in range(len(user_hands)):
            if i == blackjack.curr_hand and len(user_hands) > 1:
                val += ':white_small_square:'
            val += self._cards_to_embed_str(user_hands[i]) + '\n'
        e.add_field(name=name, value=val)

        result_values = []
        if blackjack.get_curr_state() == BJ_MOD.PLAYER_DONE:
            for i in range(len(results)):
                r = results[i]
                if r != BJ_MOD.ONGOING:
                    if r == BJ_MOD.PLAYER_WIN:
                        result_values += ['You Won!']
                    elif r == BJ_MOD.PLAYER_LOSE:
                        result_values += ['You Lost!']
                    elif r == BJ_MOD.PLAYER_BUST:
                        result_values += ['You Busted!']
                    elif r == BJ_MOD.HOUSE_BUST:
                        result_values += ['Bot Busted, You Won!']
                    elif r == BJ_MOD.DRAW:
                        result_values += ['Draw!']
        if result_values:
            value = '\n'.join(result_values)
            net = blackjack.net
            if net >= 0:
                net = f'+{net}'
            e.description = f'Bets: {"/".join([str(b) for b in bets])}'
            e.add_field(name='Results', value=f'{value}\nCredits: {user_creds}({net})', inline=False)

        e.set_footer(text=user.id)

        return e

    async def _finalize_bj(self, user, msg, embed):
        bj_state = self.bj_states.pop(user.id, None)
        await msg.clear_reactions()
        if self.cr.get_user_creds(user) >= BJ_DEFAULT_BET:
            await msg.add_reaction(AGAIN)
        return bj_state

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
            bj_game = self.bj_states[user.id]
            bj_state_final = None
            lock = bj_game.lock
            if not lock.acquire(blocking=False):
                return
            try:
                action = reaction.emoji
                error = None
                try:
                    self._check_bj_deck()
                    if action == HIT:
                        bj_game.hit()
                    elif action == DOUBLE:
                        bj_game.double()
                    elif action == HOLD:
                        bj_game.hold()
                    elif action == SPLIT:
                        bj_game.split()
                except BJ_MOD.BlackjackException as e:
                    error = e

                if bj_game.get_curr_state() == BJ_MOD.PLAYER_DONE:
                    bj_state_final = await self._finalize_bj(user, msg, embed)
                else:
                    await msg.remove_reaction(reaction, user)
                e = self._embed_from_bj(user, bj_game)
                if error:
                    e.add_field(name='Error', value=error, inline=False)
                await msg.edit(embed=e)
            finally:
                lock.release()
            if bj_state_final:
                await self.stats.save_bj_stats(bj_state_final, user.id)
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
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        user = ctx.author
        if user.id in self.bj_states.keys():
            return await ctx.send('You already have a Blackjack game running.')
        try:
            self._check_bj_deck()
            bj_game = BJ_MOD.Blackjack(self.cr, self.bj_deck, bet, self.bot.user, user)
            self.bj_states[user.id] = bj_game
        except BJ_MOD.BlackjackException as e:
            return await ctx.send(e)

        msg = await ctx.send(embed=self._embed_from_bj(user, bj_game))
        if bj_game.get_curr_state() == BJ_MOD.ONGOING:
            await msg.add_reaction(HIT)
            await msg.add_reaction(HOLD)
            await msg.add_reaction(DOUBLE)
            await msg.add_reaction(SPLIT)
        else:
            bj_state_final = await self._finalize_bj(user, msg, msg.embeds[0])
            await self.stats.save_bj_stats(bj_state_final, user.id)

    @commands.command(aliases=['gst', 'st'],
                      description='Check stats for a game',
                      help=VALID_GAMES_HELP,
                      brief='Game Stats')
    async def game_stats(self, ctx, game):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        if game in BLACKJACK:
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
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        if not user:
            user = ctx.author
        if game in BLACKJACK:
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
    Other Game Methods
    """
    @commands.command(description='See Info for Game Deck',
                      help=VALID_GAMES_HELP,
                      brief='Check Game Deck')
    async def deck(self, ctx, *, game):
        if not UTILS.can_cog_in(self, ctx.channel):
            return
        e = Embed(colour=Colour.from_rgb(255, 255, 254))
        title = '{} Deck Info'
        if game in BLACKJACK:
            title = title.format('Blackjack')
            self.bj_deck.lock.acquire()
            e.description = ''
            e.description += '```Cards Used: {:3}\n'.format(self.bj_deck.cards_used())
            e.description += 'Cards Left: {:3}```'.format(self.bj_deck.cards_left())
            self.bj_deck.lock.release()
        e.title = title
        await ctx.send(embed=e)

    @commands.command(aliases=['d'],
                      description='Get Daily Credits for Games',
                      help='24h Reset',
                      brief='Credits Daily')
    async def daily(self, ctx):
        if not UTILS.can_cog_in(self, ctx.channel):
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
        if not UTILS.can_cog_in(self, ctx.channel):
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
