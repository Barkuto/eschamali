import json
import os
import os.path
import traceback
import sys
import discord
import sqlite3
from discord import Game, DMChannel
from discord.ext import commands
from discord.ext.commands import Bot
from datetime import datetime

from util.prefix_manager import PrefixManager
from util.vars import LOGGER, COGS_DIR_NAME, COGS_DIR, BASE_COGS_DIR_NAME, LOGS_INFO_DIR, LOGS_ERROR_DIR, DB_DIR, CONFIG_FILE


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
                         activity=Game(self.config['status']))

    def all_cogs(self):
        return [file.replace('.py', '') for file in os.listdir(COGS_DIR) if file.endswith('.py')]

    def loaded_cogs(self):
        return [c.lower() for c in self.cogs]

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
        if m.startswith(self.pm.default_prefix):
            cmd = m.split(' ')[0].split(self.pm.default_prefix)[1]
            if not cmd in self.pm.cmd_prefixes['Base']:
                process = False
        if msg.guild:
            channel = f'[{msg.guild.id}]'
        elif isinstance(msg.channel, DMChannel):
            channel = f'({msg.channel.recipient})'
            if not m.startswith(self.pm.default_prefix):
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


@commands.command()
async def uptime(ctx):
    await ctx.send(str(datetime.now() - ctx.bot.start_time).split('.')[0])


@commands.command(aliases=['guilds'])
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


@commands.command(aliases=['cs'])
async def changestatus(ctx, *, msg):
    await ctx.bot.change_presence(activity=Game(msg))


@commands.command(aliases=['cds'])
async def changedefaultstatus(ctx, *, msg):
    ctx.bot.config['status'] = msg
    await changestatus(ctx, msg=msg)
    with open(CONFIG_FILE, 'w') as f:
        json.dump(ctx.bot.config, f)
    await ctx.send('Changed default status to ' + msg)


@commands.command()
async def shutdown(ctx):
    await ctx.send('Shutting down...')
    await ctx.bot.logout()


@commands.command(aliases=['git'])
async def gitup(ctx):
    stream = os.popen('git pull origin master')
    output = stream.read()
    await ctx.send(f'```{stream.read()}```')
    if output != 'Already up to date.':
        await ctx.bot.cogs['General']._reload_all(ctx)


DEFAULT_COMMANDS = [uptime, servers, changestatus, changedefaultstatus, shutdown, gitup]
Eschamali()._run()
