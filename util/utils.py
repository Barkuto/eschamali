import importlib
import discord
import base64
import json
from urllib.parse import urlparse, parse_qsl, urlencode, urlunparse, parse_qs
from discord.ext import commands
from util import db, vars

CONFIRM_EMOJI = '✅'
DENY_EMOJI = '❌'
ERR_EMOJI = '⚠️'


def reload():
    importlib.reload(db)
    importlib.reload(vars)


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
        return db.DB(f'{vars.DB_DIR}{source.guild.id}')
    elif isinstance(source, discord.TextChannel):
        return db.DB(f'{vars.DB_DIR}{source.guild.id}')
    elif isinstance(source, discord.Guild):
        return db.DB(f'{vars.DB_DIR}{source.id}')
    elif isinstance(source, int):
        return db.DB(f'{vars.DB_DIR}{source}')
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

# https://github.com/TsubakiBotPad/discord-menu/
# discordmenu/intra_message_state.py


def make_data_url(user, json_dict={}, img_url='https://i.imgur.com/0Xd1Qa6.png'):
    json_dict['user_id'] = user.id

    raw_bytes = base64.b64encode(json.dumps(json_dict).encode())
    data = str(raw_bytes)[2:-1]

    params = {'data': data}
    url_parts = list(urlparse(img_url))

    query = dict(parse_qsl(url_parts[4]))
    query.update(params)

    url_parts[4] = urlencode(query)
    return urlunparse(url_parts)


def read_data_url(url):
    parsed_url = urlparse(url)
    query_params_dict = parse_qs(parsed_url.query)
    data = query_params_dict.get('data')
    if not data:
        return None

    result_bytes = base64.b64decode(data[0])
    return json.loads(result_bytes)


def get_embed_data(embed):
    footer = embed.footer
    if footer and footer.icon_url:
        return read_data_url(footer.icon_url)
    return None
