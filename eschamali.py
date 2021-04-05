import json
import os
import os.path
import traceback
import sys
import discord
import sqlite3
from discord import Game, DMChannel, Embed
from discord.ext import commands
from discord.ext.commands import Bot
from datetime import datetime

from util.prefix_manager import PrefixManager
from util.vars import LOGGER, COGS_DIR_NAME, COGS_DIR, BASE_COGS_DIR_NAME, LOGS_INFO_DIR, LOGS_ERROR_DIR, DB_DIR, CONFIG_FILE

intents = discord.Intents.none()
intents.guilds = True
intents.members = True
intents.bans = True
intents.emojis = True
intents.voice_states = True
intents.presences = True
intents.messages = True
intents.guild_messages = True
intents.dm_messages = True
intents.reactions = True
intents.guild_reactions = True


class Eschamali(commands.Bot):
    def __init__(self):
        self.config = {}
        self.pm = PrefixManager(self)
        self.start_time = datetime.now()
        self.tbs = []
        with open(CONFIG_FILE) as f:
            self.config = json.load(f)
        super().__init__(command_prefix=self.pm.get_prefix,
                         owner_ids=self.config['owners'],
                         activity=Game(self.config['status']),
                         help_command=None,
                         intents=intents)

    def all_cogs(self):
        return [file.replace('.py', '') for file in os.listdir(COGS_DIR) if file.endswith('.py')]

    def loaded_cogs(self, lower=True):
        return [c.lower() if lower else c for c in self.cogs]

    def load_cogs(self):
        for cog in self.all_cogs():
            try:
                self.load_extension(f'{COGS_DIR_NAME}.{cog}')
            except Exception as e:
                LOGGER.error(e)

    def _run(self):
        if (self.config):
            self.start_time = datetime.now()
            self.run(self.config['token'])

    async def on_ready(self):
        for c in DEFAULT_COMMANDS:
            self.add_command(c)
        self.load_cogs()
        self.pm.load_prefixes()

    async def on_message(self, msg):
        process = True
        m = msg.content
        if m.startswith(self.pm.prefix):
            cmd = m.split(' ')[0].split(self.pm.prefix)[1]
            if not cmd in self.pm.cmd_prefixes['Base'] or cmd == 'help':
                process = False
        if msg.guild:
            channel = f'[{msg.guild.id}]'
        elif isinstance(msg.channel, DMChannel):
            channel = f'({msg.channel.recipient})'
            if not m.startswith(self.pm.prefix):
                process = False
            elif not msg.author.id in self.owner_ids:
                process = False
        else:
            channel = f'({msg.channel.name})'
        LOGGER.info('{0} {1.author}: {1.content}'.format(channel, msg))
        if msg.author.bot:
            return
        if process:
            await self.process_commands(msg)

    async def on_command_error(self, ctx, error):
        if isinstance(error, commands.CommandNotFound):
            return
        elif isinstance(error, commands.NotOwner):
            return
        elif isinstance(error, commands.CheckAnyFailure) or isinstance(error, commands.CheckFailure):
            await ctx.send(error)
        elif isinstance(error, commands.TooManyArguments):
            await ctx.send('Too many argument(s).')
        elif isinstance(error, commands.BadArgument):
            await ctx.send('Invalid argument(s).')
        elif isinstance(error, commands.MissingRequiredArgument):
            return await ctx.send('Missing argument(s).')
        elif isinstance(error, commands.CommandInvokeError):
            o_error = error.original
            if isinstance(o_error, discord.errors.Forbidden):
                if ctx.cog.qualified_name == 'Roles':
                    await ctx.send('I do not have enough permissions to add that role.')
        elif isinstance(error, discord.errors.HTTPException):
            if ctx.command.name == 'ev':
                await ctx.send('Output too big.')
        elif isinstance(error, sqlite3.Error):
            await ctx.send('There was a database error.')
        if ctx.cog:
            LOGGER.error(f'({ctx.author}){ctx.cog.qualified_name}|{ctx.message.content}|{error}')
        # All other Errors not returned come here. And we can just print the default TraceBack.
        if len(self.tbs) >= 50:
            self.tbs = []
        self.tbs.insert(0, ''.join(traceback.format_exception(type(error), error, error.__traceback__)))
        print('Ignoring exception in command {}:'.format(ctx.command), file=sys.stderr)
        traceback.print_exception(type(error), error, error.__traceback__, file=sys.stderr)


"""
HELP COMMAND FUNCTIONS
"""


@commands.command(description='Look at help for cogs and commands',
                  help='*args* can be a cog or command name, not case-sensitive')
async def help(ctx, *, args=None):
    if not args:
        return await send_bot_help(ctx)
    else:
        args = args.lower()
        for name, cog in ctx.bot.cogs.items():
            if name.lower() == args:
                return await send_cog_help(ctx, cog)
        for command in ctx.bot.commands:
            if args == command.name or (command.aliases and args in command.aliases):
                try:
                    if command.commands:
                        return await send_group_help(ctx, command)
                except:
                    return await send_cmd_help(ctx, command)
    await ctx.send('Invalid cog or command.')


async def send_bot_help(ctx):
    e = Embed(title='Eschamali Help')
    e.description = '\n'.join(sorted(ctx.bot.loaded_cogs(lower=False)))
    e.set_footer(text=f'{ctx.bot.pm.help_prefix}help <cog>')
    e.colour = discord.Colour.purple()
    await ctx.send(embed=e)


async def send_cog_help(ctx, cog):
    e = Embed(title=f'[{_get_cog_prefix(ctx, cog)}]{cog.qualified_name} Help')
    e.description = (cog.description + '\n' if cog.description else '') + \
        '\n'.join([f'**{c.name}** {c.brief if c.brief else ""}' for c in cog.get_commands()])
    e.set_footer(text=f'{ctx.bot.pm.help_prefix}help <cmd>')
    e.colour = discord.Colour.purple()
    await ctx.send(embed=e)


async def send_group_help(ctx, group):
    prefix = _get_cog_prefix(ctx, group.cog)
    e = Embed(title=f'{prefix}{group.name} subcommands')
    e.description = group.description
    for c in group.commands:
        e.add_field(name=f'{prefix}{group.name} {c.name}',
                    value=c.description + ('\nAliases: ' + ' '.join(c.aliases)) if c.aliases else '',
                    inline=True)
    e.colour = discord.Colour.purple()
    await ctx.send(embed=e)


async def send_cmd_help(ctx, command):
    prefix = _get_cog_prefix(ctx, command.cog) if command.name != 'help' else ctx.bot.pm.help_prefix
    e = Embed(title=f'{prefix}{command.name} {command.signature}')
    e.add_field(name='Description',
                value=command.description,
                inline=True)
    e.add_field(name='Help',
                value=command.help,
                inline=False)
    if command.aliases:
        e.set_footer(text=f'Aliases: {" ".join(command.aliases)}')
    e.colour = discord.Colour.purple()
    await ctx.send(embed=e)


def _get_cog_prefix(ctx, cog):
    if cog and cog.qualified_name in ctx.bot.pm.cog_prefixes:
        return ctx.bot.pm.cog_prefixes[cog.qualified_name]
    return ctx.bot.pm.prefix


"""
OWNER FUNCTIONS
"""


@commands.command(description='Shows how long the bot has been running',
                  help='Owners only',
                  brief='Show bot uptime')
@commands.is_owner()
async def uptime(ctx):
    await ctx.send(str(datetime.now() - ctx.bot.start_time).split('.')[0])


@commands.command(aliases=['guilds'],
                  description='Shows servers the bot is in',
                  help='Owners only',
                  brief='Show bot servers')
@commands.is_owner()
async def servers(ctx):
    guilds = sorted(ctx.bot.guilds, key=lambda g: g.id)
    output = f'Connected to `{len(guilds)}` guilds.\n'
    output += '```xl\n'
    output += '%s | %s | %s\n%s | %s | %s\n' % ('Server Name'.center(50),
                                                'Server ID'.center(20),
                                                'Users'.center(10),
                                                '-' * 50, '-' * 20, '-' * 10)
    for g in guilds:
        output += '%50s | %s | %s\n' % (g.name, str(g.id).center(20), str(g.member_count).center(10))
    output += '```'
    await ctx.send(output)


@commands.command(aliases=['cs'],
                  description='Change bot status for this session',
                  help='Owners only',
                  brief='Change bot status')
@commands.is_owner()
async def changestatus(ctx, *, msg):
    await ctx.bot.change_presence(activity=Game(msg))


@commands.command(aliases=['cds'],
                  description='Change default bot status for current and future sessions',
                  help='Owners only',
                  brief='Change default bot status')
@commands.is_owner()
async def changedefaultstatus(ctx, *, msg):
    ctx.bot.config['status'] = msg
    await changestatus(ctx, msg=msg)
    with open(CONFIG_FILE, 'w') as f:
        json.dump(ctx.bot.config, f)
    await ctx.send('Changed default status to ' + msg)


@commands.command(description='Shutdown the bot',
                  help='Owners only',
                  brief='Shutdown bot')
@commands.is_owner()
async def shutdown(ctx):
    await ctx.send('Shutting down...')
    await ctx.bot.close()


@commands.command(description='Update bot files with latest from github',
                  help='Owners only',
                  brief='Update from github')
@commands.is_owner()
async def git(ctx):
    stream = os.popen('git pull origin master')
    output = stream.read()
    stream2 = os.popen('git log -1')
    output2 = stream2.read().split('\n')
    output2 = '\n'.join([o.strip() for o in output2 if not o.startswith('commit ') and not o.startswith('Author') and len(o) > 0])
    await ctx.send(f'```{output}\n\n{output2}```')
    if not output.startswith('Already up to date.'):
        to_reload = []
        for line in output.split('\n'):
            if line.strip().startswith('cogs/'):
                cog = line.strip().split(' ')[0].split('/')[1].split('.')[0]
                to_reload.append(cog)
        if to_reload:
            await ctx.bot.cogs['General']._reload_all(ctx, to_reload)
        else:
            await ctx.send('No cogs to reload. Might require restart.')


DEFAULT_COMMANDS = [uptime, servers, changestatus, changedefaultstatus, shutdown, git, help]
Eschamali()._run()
