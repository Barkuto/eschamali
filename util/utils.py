import importlib
import discord
from discord.ext import commands

VARS = importlib.import_module('.vars', 'util')
DB_MOD = importlib.import_module('.db', 'util')
DB = DB_MOD.DB

CONFIRM_EMOJI = '✅'
DENY_EMOJI = '❌'
ERR_EMOJI = '⚠️'


async def confirm(ctx):
    return await ctx.message.add_reaction(CONFIRM_EMOJI)


async def deny(ctx):
    return await ctx.message.add_reaction(DENY_EMOJI)


async def err(ctx):
    return await ctx.message.add_reaction(ERR_EMOJI)


def is_cog_enabled(guild, cog):
    return cog.bot.get_cog('Perms').is_cog_enabled(guild, cog.qualified_name)


def can_cog_in(cog, channel):
    return cog.bot.get_cog('Perms').can_cog_in(cog.qualified_name, channel)


def get_server_db(source):
    if isinstance(source, commands.Context):
        return DB(f'{VARS.DB_DIR}{source.guild.id}')
    elif isinstance(source, discord.TextChannel):
        return DB(f'{VARS.DB_DIR}{source.guild.id}')
    elif isinstance(source, discord.Guild):
        return DB(f'{VARS.DB_DIR}{source.id}')
    elif isinstance(source, int):
        return DB(f'{VARS.DB_DIR}{source}')
    return None


def find_member(guild, query):
    if isinstance(query, str) and '#' in query:
        name, disc = query.lower().split('#')
        if name and disc:
            for m in guild.members:
                nick_found = name == m.nick.lower() or name in m.nick.lower() if m.nick else False
                if disc == m.discriminator and (name == m.name.lower()
                                                or name in m.name.lower()
                                                or nick_found):
                    return m
    else:
        find = discord.utils.get(guild.members, name=query)
        find = find or discord.utils.get(guild.members, id=query)
        if find:
            return find
        query = query.lower()
        # lower() doesnt work on greek symbols but shrug
        for m in guild.members:
            if m.name.lower() == query:
                return m
            elif m.nick and m.nick.lower() == query:
                return m
            elif query in m.name.lower():
                return m
            elif m.nick and query in m.nick.lower():
                return m
    return None


def find_role(guild, role):
    role_name = str(role).lower()
    for r in guild.roles:
        if r.name.lower() == role_name.lower():
            return r
    try:
        role_id = int(role)
        for r in guild.roles:
            if r.id == role_id:
                return r
    except:
        pass
    return None
