import asyncio
import math
import os.path
import traceback
import textwrap
import ast
import signal
import discord
from discord import Embed, ActivityType, Game, Colour
from discord.ext import commands
from discord.ext.commands import ExtensionError
from discord.utils import get
from datetime import datetime, timezone
from contextlib import redirect_stdout
from io import StringIO


class TimeOutException(Exception):
    pass


def timeout_handler(signum, frame):
    raise TimeOutException()


class General(commands.Cog):
    def __init__(self, bot):
        self.bot = bot
        self.last_result = None
        self.last_cmd = None

    @commands.command(ignore_extra=False,
                      description='Ping!',
                      help='Pong!',
                      brief='Ping!')
    async def ping(self, ctx):
        await ctx.send('pong!')

    @commands.command(aliases=['g'],
                      description='Get a google search link from *query*',
                      help='Let me google that for you',
                      brief='Google')
    async def google(self, ctx, *query):
        await ctx.send('https://www.google.com/search?q=' + '+'.join(query))

    @commands.command(description='Show donation link',
                      help='Optional donations',
                      brief='Donate')
    async def donate(self, ctx):
        await ctx.send('Donate for server/development funds at: https://streamelements.com/barkuto/tip')

    @commands.command(description='Show bot maker',
                      help='Yes I made this',
                      brief='Maker')
    async def maker(self, ctx):
        await ctx.send('Made by **Barkuto**#2315 specifically for Puzzle and Dragons servers. Code at https://github.com/Barkuto/Eschamali')

    @commands.command(description='*T* *I* *L* *T* *E* *D*',
                      help='*T* *I* *L* *T* *E* *D*',
                      brief='TILT')
    async def tilt(self, ctx):
        await ctx.send('*T* *I* *L* *T* *E* *D*')

    @commands.command(description='ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ',
                      help='ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ',
                      brief='RIOT')
    async def riot(self, ctx):
        await ctx.send('ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ')

    @commands.command(aliases=['uinfo'],
                      description='Show user info for user from *query*',
                      help='Get self info by giving no argument\nGet other user argument by mention, name, nickname, or name#discriminator',
                      brief='User info')
    async def userinfo(self, ctx, query=None):
        user = ctx.author
        if query:
            if ctx.message.mentions:
                user = ctx.message.mentions[0]
            else:
                user = self.bot.utils.find_member(ctx.guild, query)
                if not user:
                    return await ctx.send('Could not find that user.')
        name = user.name
        disc = user.discriminator
        nick = user.nick
        avatar = user.avatar
        created = user.created_at
        joined = user.joined_at
        roles = [r for r in sorted(user.roles, reverse=True) if not r.name == '@everyone']
        status = user.status
        activities = user.activities
        users_by_joined = sorted(ctx.guild.members, key=lambda m: m.joined_at)
        footer = f'Member #{users_by_joined.index(user) + 1} | ID: {user.id}'

        activity_str = f'**{status.name.upper()}**'
        for a in activities:
            if a.type == ActivityType.playing:
                activity_str += '\nPlaying **%s %s**' % (a.name, '' if isinstance(a, Game) else ('- ' + a.details) if a.details else '')
            elif a.type == ActivityType.streaming:
                activity_str += f'\nStreaming **{a.name}**'
            elif a.type == ActivityType.listening:
                activity_str += f'\nListening to **{a.name}**'
            elif a.type == ActivityType.watching:
                activity_str += f'\nWatching **{a.name}**'
            elif a.type == ActivityType.custom:
                activity_str += '\n**'
                if a.emoji:
                    activity_str += f'{a.emoji} '
                if a.name:
                    activity_str += a.name
                activity_str += '**'

        fmt = '%b %d, %Y %I:%M %p'
        now = datetime.now().astimezone()
        created = created.replace(tzinfo=timezone.utc).astimezone(tz=now.tzinfo)
        joined = joined.replace(tzinfo=timezone.utc).astimezone(tz=now.tzinfo)
        e = Embed(
            title=f'{name}#{disc}' + (f' AKA {nick}' if nick else ''),
            description=activity_str,
            colour=roles[0].colour if roles else 0
        ).add_field(
            name='Account Created',
            value=f'{created.strftime(fmt)}\n{(now - created).days} days ago',
            inline=True
        ).add_field(
            name='Guild Joined',
            value=f'{joined.strftime(fmt)}\n{(now - joined).days} days ago',
            inline=True
        ).set_thumbnail(url=avatar).set_footer(text=footer)
        if roles:
            e.add_field(name='Roles',
                        value=' '.join([r.mention for r in roles]),
                        inline=False)
        await ctx.send(embed=e)

    @commands.command(aliases=['sinfo'],
                      description='Show server info for current server',
                      help='No arguments',
                      brief='Server info')
    async def serverinfo(self, ctx):
        guild = ctx.guild
        name = guild.name
        desc = guild.description
        created = guild.created_at
        owner = guild.owner
        members = guild.member_count
        roles = guild.roles

        # maybe deal with dst someday
        fmt = '%b %d, %Y %I:%M %p'
        now = datetime.now().astimezone()
        created = created.replace(tzinfo=timezone.utc).astimezone(tz=now.tzinfo)
        e = Embed(
            title=name,
            description=desc
        ).add_field(
            name='Created',
            value=f'{created.strftime(fmt)}',
            inline=True
        ).add_field(
            name='Server Age',
            value=f'{(now - created).days} days',
            inline=True
        ).add_field(
            name='Owner',
            value=owner.mention,
            inline=True
        ).add_field(
            name='Members',
            value=members,
            inline=True
        ).add_field(
            name='Roles',
            value=len(roles),
            inline=True
        ).set_thumbnail(url=guild.icon).set_footer(text=guild.id)
        await ctx.send(embed=e)

    @commands.command(description='Make the bot say *msg*',
                      help='Owners Only',
                      brief='Say it')
    @commands.check_any(commands.is_owner(),
                        commands.has_permissions(manage_guild=True))
    async def say(self, ctx, *, msg):
        await ctx.message.delete()
        await ctx.send(msg)

    # Based off https://github.com/Cog-Creators/Red-DiscordBot/blob/V3/develop/redbot/core/dev_commands.py#L152
    @commands.command(aliases=['eval'],
                      description='Evaluate a string *body* with python',
                      help='Supports basic math and python operations.\npython math module is built-in\nOutput taken from "print()" and "return"\nMake sure to indent, supports code blocks',
                      brief='Evaluate')
    async def ev(self, ctx, *, body=None):
        # Set environment based on author
        if ctx.author.id in ctx.bot.owner_ids:
            if not body and self.last_cmd:
                return await self.ev(ctx, body=self.last_cmd)
            elif not body and not self.last_cmd:
                return
            self.last_cmd = body
            env = {
                '_': self.last_result,
                'asyncio': asyncio,
                'discord': discord,
                'os': os,
                'ctx': ctx,
                'bot': ctx.bot,
                'author': ctx.author,
                'channel': ctx.channel,
                'msg': ctx.message,
                'guild': ctx.guild,
                'db': self.bot.utils.get_server_db(ctx),
                'utils': self.bot.utils
            }
        else:
            if not body:
                return
            blacklist = ['import', '__', 'eval', 'exec', 'compile', 'getattr']
            run = True
            for s in blacklist:
                run = run and not s in body
            if not run:
                return await ctx.send('Nice try, Joe :smirk:')
            env = {
                '__builtins__': {},
                'print': print,
                'range': range
            }
        # Add math methods to either environment
        for fname in dir(math):
            if not '__' in fname:
                env[fname] = getattr(math, fname)

        # Remove code block from body
        # Add a return statement if code is one-liner
        if body.startswith('```') and body.endswith('```'):
            body = body.replace('```python', '').replace('```py', '')[:-3].replace('```', '')
        body = body.strip(' \n')
        body_split = body.split('\n')
        if len(body_split) == 1:
            body = f'return {body}'

        # Compile function with body, throw syntax error if bad
        stdout = StringIO()
        to_compile = 'async def func():\n%s' % textwrap.indent(body, '  ')
        signal.signal(signal.SIGALRM, timeout_handler)
        try:
            compiled = compile(to_compile, '<string>', 'exec', flags=ast.PyCF_ALLOW_TOP_LEVEL_AWAIT, optimize=0)
            exec(compiled, env)
        except SyntaxError as e:
            return await ctx.send(f'```py\n{e}```')

        func = env['func']
        result = None
        errored = False
        try:
            with redirect_stdout(stdout):
                signal.alarm(10)
                result = await func()
        except TimeOutException as e:
            printed = traceback.format_exc()
        except:
            errored = True
            printed = f'{stdout.getvalue()}{traceback.format_exc()}'
        else:
            printed = stdout.getvalue()
        signal.alarm(0)

        max_lines = 20
        split = printed.split('\n')
        if len(split) > max_lines:
            if errored:
                printed = traceback.format_exc()
            else:
                printed = '\n'.join(split[:max_lines] + ['...more output...'])

        if result is not None:
            if ctx.author.id in ctx.bot.owner_ids:
                self.last_result = result
            msg = f'{printed}{result}'
        else:
            msg = printed
        msg = msg.replace(self.bot.config['token'], '[REDACTED]')
        msg = msg.replace(os.path.join(os.path.dirname(__file__)), '....')
        if msg:
            if len(msg) >= 2000:
                await ctx.send(f'```Output too big.```')
            else:
                await ctx.send(f'```py\n{msg}```')
        else:
            await ctx.send('```No output.```')

    @commands.command(aliases=['tb'],
                      description='Shows the last *num* tracebacks',
                      help='Owners only\nDefaults to 1 traceback',
                      brief='Tracebacks')
    @commands.is_owner()
    async def traceback(self, ctx, num: int = 1):
        if self.bot.tbs:
            for i in range(0, min(len(self.bot.tbs), num)):
                split_str = '\nThe above exception was the direct cause of the following exception:\n'
                tb = self.bot.tbs[i]
                to_send = []
                if len(tb) > (2000 - 10):
                    for m in tb.split(split_str):
                        to_send.append(m)
                if to_send:
                    await ctx.send(f'```py\n{to_send[0]}```')
                    for m in to_send[1:]:
                        await ctx.send(f'```py\n{split_str}{m}```')
                else:
                    await ctx.send(f'```py\n{self.bot.tbs[i]}```')
        else:
            await ctx.send('No tracebacks.')

    @commands.command(aliases=['ctb'],
                      description='Clears all tracebacks',
                      help='Owners only\n',
                      brief='Clear Tracebacks')
    @commands.is_owner()
    async def clear_tracebacks(self, ctx):
        self.bot.tbs = []
        return await ctx.send('Tracebacks Cleared.')

    """
    COG COMMANDS
    """

    @commands.command(description='Loads *cogs*',
                      help='Owners only',
                      brief='Load cogs')
    @commands.is_owner()
    async def load(self, ctx, *cogs):
        for cog_name in cogs:
            cog_name = cog_name.lower()
            try:
                await ctx.bot.load_extension(f'{self.bot.vars.COGS_DIR_NAME}.{cog_name}')
                ctx.bot.pm.load_cog_cmd_prefixes(cog_name)
                await ctx.send(f'`{cog_name}` loaded.')
            except ExtensionError as e:
                await ctx.send(f'Error loading `{cog_name}`')
                raise e

    @commands.command(description='Reloads *cogs*',
                      help='Owners only',
                      brief='Reload cogs')
    @commands.is_owner()
    async def reload(self, ctx, *cogs):
        for cog_name in cogs:
            cog_name = cog_name.lower()
            if cog_name == 'all':
                return await self._reload_all(ctx)
            elif cog_name == 'utils':
                self.bot.reload_utils()
                return await ctx.send('Utils reloaded')
            else:
                try:
                    nick = cog_name
                    for c in self.bot.all_cogs():
                        if c.startswith(nick):
                            cog_name = c
                    if not cog_name in [k.lower() for k, _ in self.bot.cogs.items()] and cog_name in self.bot.all_cogs():
                        await self.load(ctx, cog_name)
                    elif not cog_name in self.bot.all_cogs():
                        await ctx.send('Invalid cog.')
                    else:
                        await ctx.bot.reload_extension(f'{self.bot.vars.COGS_DIR_NAME}.{cog_name}')
                        ctx.bot.pm.load_cog_cmd_prefixes(cog_name)
                        await ctx.send(f'`{cog_name}` reloaded.')
                except ExtensionError as e:
                    await ctx.send(f'Error reloading `{cog_name}`')
                    raise e

    @commands.command(description='Unloads *cogs*',
                      help='Owners only',
                      brief='Unload cogs')
    @commands.is_owner()
    async def unload(self, ctx, *cogs):
        for cog_name in cogs:
            cog_name = cog_name.lower()
            try:
                await ctx.bot.unload_extension(f'{self.bot.vars.COGS_DIR_NAME}.{cog_name}')
                ctx.bot.pm.load_cog_cmd_prefixes(cog_name)
                await ctx.send(f'`{cog_name}` unloaded.')
            except ExtensionError as e:
                await ctx.send(f'Error unloading `{cog_name}`')
                raise e

    async def _reload_all(self, ctx, reload_cogs=[]):
        all_cogs = ctx.bot.all_cogs()
        if not reload_cogs:
            reload_cogs = all_cogs
        reloaded = []
        not_reloaded = []
        for cog in reload_cogs:
            if cog in all_cogs:
                try:
                    await ctx.bot.reload_extension(f'{self.bot.vars.COGS_DIR_NAME}.{cog}')
                    ctx.bot.pm.load_cog_cmd_prefixes(cog)
                    reloaded.append(cog)
                except ExtensionError:
                    not_reloaded.append(cog)
        reloaded = [f'`{c}`' for c in reloaded]
        not_reloaded = [f'`{c}`' for c in not_reloaded]
        if reloaded:
            await ctx.send(f'Reloaded {" ".join(reloaded)}')
        if not_reloaded:
            await ctx.send(f'Could not reload {" ".join(not_reloaded)}')

    @commands.command(description='Shows all cogs',
                      help='No arguments',
                      brief='Show cogs')
    async def cogs(self, ctx):
        loaded = [k.lower() for k, _ in ctx.bot.cogs.items()]
        all_cogs = self.bot.all_cogs()
        unloaded = []
        for c in all_cogs:
            if not c in loaded:
                unloaded.append(c)
        loaded = [f'`{c}`' for c in loaded]
        unloaded = [f'`{c}`' for c in unloaded]
        if loaded:
            await ctx.send(embed=Embed(
                title='Loaded Cogs',
                description=' '.join(loaded),
                colour=Colour.green()
            ))
        if unloaded:
            await ctx.send(embed=Embed(
                title='Unloaded Cogs',
                description=' '.join(unloaded),
                colour=Colour.red()))


async def setup(bot):
    await bot.add_cog(General(bot))
